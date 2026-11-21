//! Zyro provisions exactly one product per supported Minecraft version: "Zyro Client".
//! Upstream OneClient created a cluster for every Polyfrost bundle/version entry; Zyro does not.

use zyro_db::dao::cluster as cluster_dao;

use crate::LauncherResult;
use crate::game::zyro_runtime::{ZYRO_PRODUCT_NAME, ZYRO_VERSIONS};
use crate::state::LauncherState;
use zyro_common::domain::GameLoader;

use zyro_cluster::Cluster;
use zyro_cluster::CreateClusterOptions;

/// Kept for callers of the old bundle-driven provisioning.
#[tracing::instrument(skip(state))]
pub async fn ensure_from_bundles(state: &LauncherState) -> LauncherResult<Vec<Cluster>> {
    ensure_zyro_clusters(state).await
}

/// Kept for callers of the old versions-manifest provisioning.
#[tracing::instrument(skip(state))]
pub async fn ensure_from_versions(state: &LauncherState) -> LauncherResult<Vec<Cluster>> {
    ensure_zyro_clusters(state).await
}

#[tracing::instrument(skip(state))]
pub async fn ensure_zyro_clusters(state: &LauncherState) -> LauncherResult<Vec<Cluster>> {
    // create_provisioned takes the provisioning lock itself; taking it here would deadlock.
    let mut created = Vec::new();

    for mc_version in ZYRO_VERSIONS {
        let loader = GameLoader::Fabric;
        if cluster_dao::find_by_version_loader(&state.services.db, mc_version, loader as i64)
            .await?
            .is_some()
        {
            continue;
        }

        let name = format!("{ZYRO_PRODUCT_NAME} {mc_version}");
        let global = state.settings.read().global_game_settings.clone();
        match state
            .clusters
            .create_provisioned(
                &global,
                CreateClusterOptions::new(name, (*mc_version).to_string(), loader),
            )
            .await
        {
            Ok(Some(cluster)) => {
                tracing::info!(cluster_id = cluster.id, mc_version, "created Zyro Client cluster");
                created.push(cluster);
            }
            Ok(None) => {}
            Err(err) => {
                tracing::warn!(mc_version, error = %err, "failed to create Zyro Client cluster");
            }
        }
    }

    Ok(created)
}
