package dev.zyro.hud.element;

import dev.zyro.hud.render.ClickTracker;
import dev.zyro.hud.render.Ctx;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The simple text elements. */
public final class Elements {
    private Elements() {}

    public static List<HudElement> all() {
        List<HudElement> list = new ArrayList<>();
        list.add(new Fps());
        list.add(new Cps());
        list.add(new Ping());
        list.add(new Coords());
        list.add(new Speed());
        list.add(new Clock());
        list.add(new KeystrokesElement());
        list.add(new ArmorElement());
        return list;
    }

    static final class Fps extends TextElement {
        Fps() { super("fps", "FPS"); }

        @Override
        protected List<Line> lines(Ctx ctx) {
            return List.of(Line.suffix(Integer.toString(ctx.mc.getFps()), "FPS"));
        }
    }

    static final class Cps extends TextElement {
        Cps() { super("cps", "CPS"); }

        @Override
        protected List<Line> lines(Ctx ctx) {
            return List.of(Line.suffix(ClickTracker.left() + " | " + ClickTracker.right(), "CPS"));
        }
    }

    static final class Ping extends TextElement {
        Ping() { super("ping", "Ping"); }

        @Override
        protected List<Line> lines(Ctx ctx) {
            int ms = 0;
            LocalPlayer player = ctx.mc.player;
            if (player != null && ctx.mc.getConnection() != null) {
                PlayerInfo info = ctx.mc.getConnection().getPlayerInfo(player.getUUID());
                if (info != null) ms = info.getLatency();
            }
            return List.of(Line.suffix(Integer.toString(ms), "ms"));
        }
    }

    static final class Coords extends TextElement {
        Coords() { super("coords", "Coordinates"); }

        @Override
        protected List<Line> lines(Ctx ctx) {
            LocalPlayer p = ctx.mc.player;
            if (p == null) {
                return List.of(Line.prefix("X", "128"), Line.prefix("Y", "64"), Line.prefix("Z", "-256"), Line.prefix("F", "North"));
            }
            String facing = p.getDirection().getName();
            facing = facing.substring(0, 1).toUpperCase(Locale.ROOT) + facing.substring(1);
            return List.of(
                    Line.prefix("X", String.format(Locale.ROOT, "%.1f", p.getX())),
                    Line.prefix("Y", String.format(Locale.ROOT, "%.1f", p.getY())),
                    Line.prefix("Z", String.format(Locale.ROOT, "%.1f", p.getZ())),
                    Line.prefix("F", facing));
        }
    }

    static final class Speed extends TextElement {
        private double smoothed;

        Speed() { super("speed", "Speed"); }

        @Override
        protected List<Line> lines(Ctx ctx) {
            LocalPlayer p = ctx.mc.player;
            double bps = 0;
            if (p != null) {
                double dx = p.getX() - p.xo;
                double dz = p.getZ() - p.zo;
                bps = Math.sqrt(dx * dx + dz * dz) * 20.0;
            }
            smoothed += (bps - smoothed) * Math.min(1.0, ctx.dt * 8.0);
            return List.of(Line.suffix(String.format(Locale.ROOT, "%.2f", smoothed), "b/s"));
        }
    }

    static final class Clock extends TextElement {
        private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

        Clock() { super("clock", "Clock"); }

        @Override
        protected List<Line> lines(Ctx ctx) {
            return List.of(Line.suffix(LocalTime.now().format(FORMAT), ""));
        }
    }
}
