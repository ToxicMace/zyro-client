//! The Zyro Client runtime: what turns a plain Fabric instance into "Zyro Client".
//!
//! On launch the launcher assembles a per-version folder of runtime mods
//! (the embedded Zyro Client mod plus Fabric API and Sodium from Modrinth) and hands it to
//! Fabric through `fabric.addMods`. Nothing is written into the instance's own mods folder,
//! so the player's mods stay untouched and the runtime can be updated independently.

use std::path::{Path, PathBuf};

use serde::Deserialize;
use zyro_common::domain::GameLoader;
use zyro_common::paths;

pub const ZYRO_PRODUCT_NAME: &str = "Zyro Client";

/// Minecraft versions Zyro Client ships for, newest first.
pub const ZYRO_VERSIONS: &[&str] = &["26.3", "1.21.11"];

const MOD_1_21_11: &[u8] = include_bytes!("../../assets/mods/zyro-client-1.21.11.jar");
const MOD_26_3: &[u8] = include_bytes!("../../assets/mods/zyro-client-26.3.jar");

/// Modrinth projects bundled with every Zyro Client launch.
const MODRINTH_RUNTIME: &[RuntimeProject] = &[
    RuntimeProject { slug: "fabric-api", file_prefix: "fabric-api" },
    RuntimeProject { slug: "sodium", file_prefix: "sodium" },
];

const USER_AGENT: &str = concat!("ZyroClient/", env!("CARGO_PKG_VERSION"), " (launcher)");

struct RuntimeProject {
    slug: &'static str,
    file_prefix: &'static str,
}

pub fn is_zyro_version(loader: GameLoader, mc_version: &str) -> bool {
    loader == GameLoader::Fabric && ZYRO_VERSIONS.contains(&mc_version)
}

fn embedded_mod(mc_version: &str) -> Option<&'static [u8]> {
    match mc_version {
        "1.21.11" => Some(MOD_1_21_11),
        "26.3" => Some(MOD_26_3),
        _ => None,
    }
}

/// Lowercased jar names already in the instance's mods folder.
async fn instance_mods(mods_dir: &Path) -> Vec<String> {
    let mut names = Vec::new();
    let Ok(mut entries) = tokio::fs::read_dir(mods_dir).await else {
        return names;
    };
    while let Ok(Some(entry)) = entries.next_entry().await {
        let name = entry.file_name().to_string_lossy().to_ascii_lowercase();
        if name.ends_with(".jar") {
            names.push(name);
        }
    }
    names
}

fn runtime_dir(mc_version: &str) -> std::io::Result<PathBuf> {
    Ok(paths::caches_dir()
        .map_err(|err| std::io::Error::other(err.to_string()))?
        .join("zyro")
        .join("runtime")
        .join(mc_version))
}

async fn write_if_changed(path: &Path, bytes: &[u8]) -> std::io::Result<()> {
    let same = tokio::fs::metadata(path)
        .await
        .is_ok_and(|meta| meta.len() == bytes.len() as u64);
    if !same {
        tokio::fs::write(path, bytes).await?;
    }
    Ok(())
}

#[derive(Deserialize)]
struct ModrinthVersion {
    version_type: String,
    files: Vec<ModrinthFile>,
}

#[derive(Deserialize)]
struct ModrinthFile {
    url: String,
    filename: String,
    primary: bool,
    hashes: ModrinthHashes,
}

#[derive(Deserialize)]
struct ModrinthHashes {
    sha1: String,
}

fn sha1_hex(bytes: &[u8]) -> String {
    use sha1::{Digest, Sha1};
    let digest = Sha1::digest(bytes);
    digest.iter().map(|b| format!("{b:02x}")).collect()
}

/// Makes sure `dir` holds a jar for `project` that runs on `mc_version`.
async fn ensure_modrinth(
    client: &reqwest::Client,
    dir: &Path,
    project: &RuntimeProject,
    mc_version: &str,
) -> Result<PathBuf, String> {
    // Reuse what we already downloaded; runtime jars are refreshed when missing.
    if let Ok(mut entries) = tokio::fs::read_dir(dir).await {
        while let Ok(Some(entry)) = entries.next_entry().await {
            let name = entry.file_name().to_string_lossy().to_ascii_lowercase();
            if name.starts_with(project.file_prefix) && name.ends_with(".jar") {
                return Ok(entry.path());
            }
        }
    }

    let url = format!(
        "https://api.modrinth.com/v2/project/{}/version?loaders=%5B%22fabric%22%5D&game_versions=%5B%22{}%22%5D",
        project.slug, mc_version
    );
    let versions: Vec<ModrinthVersion> = client
        .get(&url)
        .send()
        .await
        .map_err(|err| err.to_string())?
        .error_for_status()
        .map_err(|err| err.to_string())?
        .json()
        .await
        .map_err(|err| err.to_string())?;

    let chosen = versions
        .iter()
        .find(|v| v.version_type == "release")
        .or_else(|| versions.first())
        .ok_or_else(|| format!("{} has no build for Minecraft {mc_version}", project.slug))?;
    let file = chosen
        .files
        .iter()
        .find(|f| f.primary)
        .or_else(|| chosen.files.first())
        .ok_or_else(|| format!("{} release has no files", project.slug))?;

    let bytes = client
        .get(&file.url)
        .send()
        .await
        .map_err(|err| err.to_string())?
        .error_for_status()
        .map_err(|err| err.to_string())?
        .bytes()
        .await
        .map_err(|err| err.to_string())?;
    if sha1_hex(&bytes) != file.hashes.sha1.to_ascii_lowercase() {
        return Err(format!("{} download failed its checksum", project.slug));
    }

    let name = sanitize(&file.filename);
    let path = dir.join(if name.to_ascii_lowercase().starts_with(project.file_prefix) {
        name
    } else {
        format!("{}-{name}", project.file_prefix)
    });
    tokio::fs::write(&path, &bytes).await.map_err(|err| err.to_string())?;
    tracing::info!(project = project.slug, mc_version, file = %path.display(), "downloaded Zyro runtime mod");
    Ok(path)
}

fn sanitize(name: &str) -> String {
    name.chars()
        .map(|c| if c.is_ascii_alphanumeric() || matches!(c, '.' | '-' | '_' | '+') { c } else { '_' })
        .collect()
}

/// Returns the JVM argument that loads the Zyro Client runtime, or None when it doesn't apply.
pub async fn jvm_argument(
    enabled: bool,
    loader: GameLoader,
    mc_version: &str,
    mods_dir: &Path,
) -> Option<String> {
    if !enabled || !is_zyro_version(loader, mc_version) {
        return None;
    }
    let jar = embedded_mod(mc_version)?;

    let dir = match runtime_dir(mc_version) {
        Ok(dir) => dir,
        Err(err) => {
            tracing::warn!("zyro runtime folder unavailable: {err}");
            return None;
        }
    };
    if let Err(err) = tokio::fs::create_dir_all(&dir).await {
        tracing::warn!("could not create the zyro runtime folder: {err}");
        return None;
    }

    let existing = instance_mods(mods_dir).await;
    let has = |prefix: &str| existing.iter().any(|name| name.starts_with(prefix));
    let mut files: Vec<PathBuf> = Vec::new();

    if !has("zyro-client") {
        let path = dir.join(format!("zyro-client-{mc_version}.jar"));
        match write_if_changed(&path, jar).await {
            Ok(()) => files.push(path),
            Err(err) => tracing::warn!("could not write the Zyro Client mod: {err}"),
        }
    }

    let client = match reqwest::Client::builder().user_agent(USER_AGENT).build() {
        Ok(client) => client,
        Err(err) => {
            tracing::warn!("zyro runtime http client failed: {err}");
            return None;
        }
    };
    for project in MODRINTH_RUNTIME {
        if has(project.file_prefix) {
            continue;
        }
        match ensure_modrinth(&client, &dir, project, mc_version).await {
            Ok(path) => files.push(path),
            Err(err) => tracing::warn!(project = project.slug, "zyro runtime mod skipped: {err}"),
        }
    }

    // The Zyro mod needs Fabric API; without it Fabric would refuse to start.
    let fabric_api_ready = has("fabric-api")
        || files.iter().any(|p| {
            p.file_name()
                .is_some_and(|n| n.to_string_lossy().to_ascii_lowercase().starts_with("fabric-api"))
        });
    if !fabric_api_ready {
        files.retain(|p| {
            !p.file_name()
                .is_some_and(|n| n.to_string_lossy().starts_with("zyro-client"))
        });
    }
    if files.is_empty() {
        return None;
    }

    let separator = if cfg!(windows) { ";" } else { ":" };
    let list = files
        .iter()
        .map(|p| p.display().to_string())
        .collect::<Vec<_>>()
        .join(separator);
    Some(format!("-Dfabric.addMods={list}"))
}
