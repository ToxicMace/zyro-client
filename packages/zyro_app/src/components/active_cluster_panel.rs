use freya::animation::*;
use freya::prelude::*;
use freya::router::RouterContext;

use crate::components::{Button, Icon, IconType, LobbyHeroChip};
use zyro_common::parse_mc_version;
use zyro_core::clusters::ClusterKind;

use crate::hooks::{
    settled_or_loading, use_active_cluster_id, use_clusters, use_dispatch, use_game_snapshot,
    use_launcher, use_version_metadata,
};
use crate::i18n::t;
use crate::routes::Route;
use crate::theme::{colors, text_size};
use crate::utils::sort_clusters_for_home;
use crate::view::app::{launch_button_state, launch_syncing, shared_dir_taken};

#[derive(PartialEq)]
pub struct ActiveClusterPanel;

impl Component for ActiveClusterPanel {
    fn render(&self) -> impl IntoElement {
        let clusters_query = use_clusters();
        let mut active_id = use_active_cluster_id();
        let dispatch = use_dispatch();
        let game = use_game_snapshot();
        let launcher = use_launcher();

        let clusters = settled_or_loading(&clusters_query).unwrap_or_default();

        let sorted = sort_clusters_for_home(clusters);

        if active_id.read().is_none()
            && let Some(first) = sorted.first()
        {
            *active_id.write() = Some(first.id);
        }

        let active = active_id
            .read()
            .and_then(|id| sorted.iter().find(|c| c.id == id).cloned())
            .or_else(|| sorted.first().cloned());

        let dep = active.as_ref().map(|c| c.id);
        let intro = use_animation_with_dependencies(&dep, |conf, _| {
            conf.on_creation(OnCreation::Run);
            conf.on_change(OnChange::Rerun);
            AnimNum::new(0., 1.)
                .time(440)
                .ease(Ease::Out)
                .function(Function::Cubic)
        });
        let p = intro.get().value();
        let slide_x = (p - 1.0) * 48.0;

        let parsed = active
            .as_ref()
            .and_then(|c| parse_mc_version(&c.mc_version));
        let metadata = use_version_metadata(
            parsed.as_ref().map(|p| p.major),
            parsed.and_then(|p| p.key()),
            active.as_ref().map(|c| c.mc_loader),
        );

        let Some(cluster) = active else {
            return rect()
                .vertical()
                .width(Size::fill())
                .main_align(Alignment::Center)
                .child(
                    label()
                        .text(t("oneclient.components.active_cluster_panel.empty"))
                        .font_size(text_size(24.))
                        .color(colors::on_art_secondary()),
                )
                .child(LobbyHeroChip);
        };

        let version = format!("{} {}", cluster.mc_version, cluster.mc_loader);
        let zyro = !cluster.user_created
            && zyro_core::game::zyro_runtime::is_zyro_version(cluster.mc_loader, &cluster.mc_version);
        let (title, subtitle) = if zyro {
            (
                zyro_core::game::zyro_runtime::ZYRO_PRODUCT_NAME.to_string(),
                format!("Minecraft {}", cluster.mc_version),
            )
        } else if cluster.user_created && cluster.kind != ClusterKind::OneClient {
            (cluster.name.clone(), version)
        } else {
            let mut parts = vec!["Zyro".to_string()];
            let title = if cluster.user_created {
                parts.push(version);
                cluster.name.clone()
            } else {
                version
            };
            parts.extend(metadata.map(|m| m.name.clone()));
            (title, parts.join(" · "))
        };
        let cluster_id = cluster.id;
        let syncing = launch_syncing(&launcher, cluster.uses_bundles());
        let shared_dir_taken = shared_dir_taken(&game, &sorted, cluster_id);

        // Zyro hero: status chip, big title and actions on a soft glass card.
        let eyebrow = rect()
            .horizontal()
            .cross_align(Alignment::Center)
            .spacing(8.)
            .padding(Gaps::new_symmetric(5., 12.))
            .corner_radius(CornerRadius::new_all(999.))
            .background(Color::from_argb(70, 255, 255, 255))
            .child(
                rect()
                    .width(Size::px(7.))
                    .height(Size::px(7.))
                    .corner_radius(CornerRadius::new_all(4.))
                    .background(colors::brand()),
            )
            .child(
                label()
                    .text(subtitle.to_uppercase())
                    .max_lines(1)
                    .font_size(text_size(11.))
                    .font_weight(FontWeight::SEMI_BOLD)
                    .color(colors::on_art()),
            );

        let card = rect()
            .vertical()
            .spacing(14.)
            .padding(Gaps::new(24., 30., 26., 28.))
            .corner_radius(CornerRadius::new_all(26.))
            .background(Color::from_argb(105, 8, 10, 18))
            .border(
                Border::new()
                    .fill(Color::from_argb(30, 255, 255, 255))
                    .width(1.)
                    .alignment(BorderAlignment::Inner),
            )
            .child(eyebrow)
            .child(
                label()
                    .text(title)
                    .max_lines(1)
                    .text_overflow(TextOverflow::Ellipsis)
                    .font_size(text_size(58.))
                    .line_height(1.05)
                    .font_weight(FontWeight::EXTRA_BOLD)
                    .color(colors::on_art()),
            )
            .child(
                rect()
                    .horizontal()
                    .spacing(10.)
                    .cross_align(Alignment::Center)
                    .child(launch_button(
                        cluster_id,
                        dispatch,
                        launch_button_state(&game, cluster_id, syncing, shared_dir_taken),
                    ))
                    .child(cluster_settings_button(cluster_id)),
            );

        rect()
            .vertical()
            .width(Size::fill())
            .main_align(Alignment::Center)
            .cross_align(Alignment::Start)
            .offset_x(slide_x)
            .opacity(p)
            .child(card)
            .child(LobbyHeroChip)
    }
}

fn launch_button(
    cluster_id: i64,
    dispatch: crate::Actions,
    state: (&'static str, bool),
) -> impl IntoElement {
    let (launch_text, enabled) = state;
    Button::new()
        .primary()
        .medium()
        .enabled(enabled)
        .padding(Gaps::new_symmetric(11., 30.))
        .on_press(move |_| {
            if enabled {
                dispatch.launch_cluster(cluster_id);
            }
        })
        .child(
            rect()
                .horizontal()
                .cross_align(Alignment::Center)
                .spacing(10.)
                .child(Icon::new(IconType::Play).size(16.).color(colors::on_brand_strong()))
                .child(
                    label()
                        .text(launch_text)
                        .font_size(text_size(16.))
                        .font_weight(FontWeight::SEMI_BOLD)
                        .color(colors::on_brand_strong()),
                ),
        )
}

fn cluster_settings_button(cluster_id: i64) -> impl IntoElement {
    Button::new()
        .ghost()
        .icon()
        .foreground(colors::on_art())
        .tooltip(t("oneclient.components.active_cluster_panel.settings"))
        .on_press(move |_| {
            let _ = RouterContext::get().push(Route::ClusterOverview { cluster_id });
        })
        .child(Icon::new(IconType::Settings04).size(20.))
}
