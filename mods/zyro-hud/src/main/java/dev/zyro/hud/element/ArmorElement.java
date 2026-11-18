package dev.zyro.hud.element;

import dev.zyro.hud.render.Ctx;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Helmet to boots plus main hand, each with remaining durability. */
public final class ArmorElement extends HudElement {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND,
    };

    public ArmorElement() {
        super("armor", "Armor");
    }

    private List<ItemStack> stacks(Ctx ctx) {
        List<ItemStack> out = new ArrayList<>();
        LocalPlayer p = ctx.mc.player;
        if (p != null) {
            for (EquipmentSlot slot : SLOTS) {
                ItemStack s = p.getItemBySlot(slot);
                if (!s.isEmpty()) out.add(s);
            }
        }
        if (out.isEmpty() && ctx.preview) {
            out.add(new ItemStack(Items.DIAMOND_HELMET));
            out.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
            out.add(new ItemStack(Items.DIAMOND_LEGGINGS));
            out.add(new ItemStack(Items.DIAMOND_BOOTS));
            out.add(new ItemStack(Items.DIAMOND_SWORD));
        }
        return out;
    }

    private static String durability(ItemStack s) {
        if (!s.isDamageableItem()) {
            return s.getCount() > 1 ? Integer.toString(s.getCount()) : "";
        }
        return Integer.toString(s.getMaxDamage() - s.getDamageValue());
    }

    @Override
    public boolean hasContent(Ctx ctx) {
        return !stacks(ctx).isEmpty();
    }

    @Override
    public int[] measure(Ctx ctx) {
        List<ItemStack> list = stacks(ctx);
        int text = 0;
        for (ItemStack s : list) text = Math.max(text, ctx.width(durability(s)));
        int p = ctx.cfg.padding;
        return new int[] {16 + (text > 0 ? text + 3 : 0) + p * 2 + barInset(ctx), Math.max(1, list.size()) * 17 - 1 + p * 2};
    }

    @Override
    public void draw(Ctx ctx, int w, int h) {
        int p = ctx.cfg.padding;
        int x = p + barInset(ctx);
        int y = p;
        for (ItemStack s : stacks(ctx)) {
            ctx.g.renderItem(s, x, y);
            String d = durability(s);
            if (!d.isEmpty()) {
                int color = ctx.text;
                if (s.isDamageableItem() && s.getMaxDamage() > 0) {
                    float left = 1f - (float) s.getDamageValue() / s.getMaxDamage();
                    if (left < 0.25f) color = 0xFFFF5A5A;
                    else if (left < 0.5f) color = 0xFFFFC857;
                }
                ctx.text(d, x + 19, y + 4, color);
            }
            y += 17;
        }
    }
}
