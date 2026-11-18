pub const WINDOW_APP_ID: &str = "zyro_app";
pub const WINDOW_TITLE: &str = "Zyro Client";

pub const UPDATER_PUBKEY: &str = "dW50cnVzdGVkIGNvbW1lbnQ6IG1pbmlzaWduIHB1YmxpYyBrZXk6IDFGODk3MkMyMjg0MjFDMDUKUldRRkhFSW93bktKSHpkWjNEMXNzaDVINVpCTU8xSnhuK2RnV0dTZ2FkcFJWbG1zUkhGYTNjaUkK";
pub const UPDATER_ENDPOINT: &str =
    match option_env!("ZYRO_UPDATER_ENDPOINT") {
    Some(url) => url,
    None => "",
};
pub const RELEASES_URL: &str = "https://github.com/Polyfrost/OneLauncher/releases/latest";

/// Poly+ (Polyfrost's paid cosmetics, wardrobe and social service) is hidden in Zyro builds.
pub const POLYPLUS_FEATURES: bool = false;
