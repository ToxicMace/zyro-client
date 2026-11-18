use crate::LauncherResult;
use zyro_net::RequestClient;

/// Zyro ships its release notes inside the binary, so the launcher never shows
/// upstream OneClient notes as if they were Zyro's.
const CHANGELOG_JSON: &str = include_str!("../assets/changelog.json");

#[derive(Debug, Clone, PartialEq, Eq, serde::Deserialize)]
pub struct ChangelogEntry {
    pub version: String,
    pub body: String,
}

#[tracing::instrument(level = "debug", skip(_net))]
pub async fn fetch_changelog(_net: &RequestClient) -> LauncherResult<Vec<ChangelogEntry>> {
    Ok(serde_json::from_str(CHANGELOG_JSON)?)
}
