//! Zyro HUD: the built-in in-game HUD mod.
//!
//! The jar ships inside the launcher and is loaded through Fabric's `fabric.addMods`
//! property, so it never touches (or gets swept out of) the instance's own mods folder.

use std::path::{Path, PathBuf};

use zyro_common::domain::GameLoader;
use zyro_common::paths;

const JAR: &[u8] = include_bytes!("../../assets/mods/zyro-hud.jar");
const JAR_NAME: &str = "zyro-hud-1.0.0.jar";
/// Minecraft versions the bundled jar is built for.
const SUPPORTED: &[&str] = &["1.21.11"];

pub fn supports(loader: GameLoader, mc_version: &str) -> bool {
    loader == GameLoader::Fabric && SUPPORTED.contains(&mc_version)
}

/// Zyro HUD needs Fabric API; only inject it when the instance already has it.
async fn has_fabric_api(mods_dir: &Path) -> bool {
    let Ok(mut entries) = tokio::fs::read_dir(mods_dir).await else {
        return false;
    };
    while let Ok(Some(entry)) = entries.next_entry().await {
        let name = entry.file_name().to_string_lossy().to_ascii_lowercase();
        if name.starts_with("fabric-api") && name.ends_with(".jar") {
            return true;
        }
    }
    false
}

async fn materialize() -> std::io::Result<PathBuf> {
    let dir = paths::caches_dir()
        .map_err(|err| std::io::Error::other(err.to_string()))?
        .join("zyro");
    tokio::fs::create_dir_all(&dir).await?;
    let path = dir.join(JAR_NAME);
    let up_to_date = tokio::fs::metadata(&path)
        .await
        .is_ok_and(|meta| meta.len() == JAR.len() as u64);
    if !up_to_date {
        tokio::fs::write(&path, JAR).await?;
    }
    Ok(path)
}

/// Returns the JVM argument that loads Zyro HUD, or None when it doesn't apply.
pub async fn jvm_argument(
    enabled: bool,
    loader: GameLoader,
    mc_version: &str,
    mods_dir: &Path,
) -> Option<String> {
    if !enabled || !supports(loader, mc_version) {
        return None;
    }
    if !has_fabric_api(mods_dir).await {
        tracing::info!("zyro hud skipped: the instance has no Fabric API");
        return None;
    }
    match materialize().await {
        Ok(path) => Some(format!("-Dfabric.addMods={}", path.display())),
        Err(err) => {
            tracing::warn!("zyro hud could not be prepared: {err}");
            None
        }
    }
}
