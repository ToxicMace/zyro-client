//! Manual network check for the Cloudflare fallback route.
//! Run with: cargo test -p zyro_net --test cloudflare_fallback -- --ignored --nocapture

use reqwest::Method;
use zyro_net::{NetConfig, RequestClient};

#[tokio::test]
#[ignore = "touches the network"]
async fn fabric_meta_is_reachable_even_when_cloudflare_ips_are_blocked() {
    let client = RequestClient::new(NetConfig::default()).expect("client");
    let url = "https://meta.fabricmc.net/v2/versions/loader".parse().unwrap();
    let loaders: Vec<serde_json::Value> = client
        .send_json(Method::GET, url, None, &[])
        .await
        .expect("fabric meta should answer, directly or through the fallback");
    assert!(!loaders.is_empty());
    println!("fabric loaders: {}", loaders.len());
}
