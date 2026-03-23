package com.admintools.server;

import com.admintools.AdminFeature;
import com.admintools.AdminToolsMod;
import com.admintools.AdminToolsState;
import com.admintools.TopLuckEntry;
import com.admintools.network.NetworkHandler;
import com.admintools.network.packets.C2STogglePacket;
import com.admintools.network.packets.S2CInvSeePacket;
import com.admintools.network.packets.S2CStatePacket;
import com.admintools.network.packets.S2CTopLuckPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

@Mod.EventBusSubscriber(modid = AdminToolsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AdminCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("at")
            .requires(src -> src.hasPermission(2))
            .executes(AdminCommand::cmdStatus)

            .then(Commands.literal("status")
                .executes(AdminCommand::cmdStatus)
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(playerSuggest())
                    .executes(ctx -> cmdStatusTarget(ctx, StringArgumentType.getString(ctx,"player")))))

            .then(Commands.literal("toggle")
                .then(Commands.argument("feature", StringArgumentType.word())
                    .suggests(featureSuggest())
                    .executes(ctx -> cmdToggle(ctx, null))
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(playerSuggest())
                        .executes(ctx -> cmdToggle(ctx, StringArgumentType.getString(ctx,"player"))))))

            .then(Commands.literal("on")
                .then(Commands.argument("feature", StringArgumentType.word())
                    .suggests(featureSuggest())
                    .executes(ctx -> cmdSet(ctx, true, null))
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(playerSuggest())
                        .executes(ctx -> cmdSet(ctx, true, StringArgumentType.getString(ctx,"player"))))))

            .then(Commands.literal("off")
                .then(Commands.argument("feature", StringArgumentType.word())
                    .suggests(featureSuggest())
                    .executes(ctx -> cmdSet(ctx, false, null))
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(playerSuggest())
                        .executes(ctx -> cmdSet(ctx, false, StringArgumentType.getString(ctx,"player"))))))

            .then(Commands.literal("tp")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(playerSuggest())
                    .executes(AdminCommand::cmdTp)))

            .then(Commands.literal("invsee")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(playerSuggest())
                    .executes(AdminCommand::cmdInvSee)))

            .then(Commands.literal("topluck")
                .executes(AdminCommand::cmdTopLuck)
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(playerSuggest())
                        .executes(AdminCommand::cmdTopLuckReset))))

            .then(Commands.literal("check")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(playerSuggest())
                    .executes(AdminCommand::cmdCheck)))

            .then(Commands.literal("help")
                .executes(AdminCommand::cmdHelp))
        );
    }

    private static com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> featureSuggest() {
        return (ctx, b) -> {
            for (AdminFeature f : AdminFeature.values()) b.suggest(f.name().toLowerCase());
            return b.buildFuture();
        };
    }

    private static com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> playerSuggest() {
        return (ctx, b) -> {
            ctx.getSource().getServer().getPlayerList().getPlayers()
               .forEach(p -> b.suggest(p.getName().getString()));
            return b.buildFuture();
        };
    }

    private static int cmdStatus(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer(); if (p == null) return 0;
        return cmdStatusTarget(ctx, p.getName().getString());
    }

    private static int cmdStatusTarget(CommandContext<CommandSourceStack> ctx, String name) {
        ServerPlayer t = ctx.getSource().getServer().getPlayerList().getPlayerByName(name);
        if (t == null) { ctx.getSource().sendFailure(Component.literal("\u00a7cJoueur introuvable : " + name)); return 0; }
        ctx.getSource().sendSuccess(() -> Component.literal(
            "\u00a78[\u00a7bAT\u00a78] Features de \u00a7f" + name + "\u00a77 :"), false);
        for (AdminFeature f : AdminFeature.values()) {
            boolean on = AdminToolsState.isEnabled(t.getUUID(), f);
            ctx.getSource().sendSuccess(() -> Component.literal(
                (on ? "  \u00a7a\u2714 " : "  \u00a7c\u2718 ")
                + "\u00a7f" + f.name().toLowerCase() + " \u00a78\u2014 \u00a77" + f.displayName), false);
        }
        return 1;
    }

    private static int cmdToggle(CommandContext<CommandSourceStack> ctx, String targetName) {
        ServerPlayer req = ctx.getSource().getPlayer(); if (req == null) return 0;
        AdminFeature f = parseFeature(StringArgumentType.getString(ctx,"feature"), ctx); if (f==null) return 0;
        ServerPlayer t = resolveTarget(ctx, targetName); if (t == null) return 0;
        boolean now = AdminToolsState.toggle(t.getUUID(), f);
        C2STogglePacket.applyEffect(t, f, now);
        syncToClient(t);
        String who = t==req ? "" : " \u00a78[\u00a7f"+t.getName().getString()+"\u00a78]";
        ctx.getSource().sendSuccess(() -> Component.literal(
            "\u00a78[AT] \u00a7f"+f.displayName+" : "+(now?"\u00a7aactive":"\u00a7cdesactive")+who), false);
        return 1;
    }

    private static int cmdSet(CommandContext<CommandSourceStack> ctx, boolean enable, String targetName) {
        ServerPlayer req = ctx.getSource().getPlayer(); if (req == null) return 0;
        AdminFeature f = parseFeature(StringArgumentType.getString(ctx,"feature"), ctx); if (f==null) return 0;
        ServerPlayer t = resolveTarget(ctx, targetName); if (t == null) return 0;
        if (AdminToolsState.isEnabled(t.getUUID(), f) != enable) {
            AdminToolsState.toggle(t.getUUID(), f);
            C2STogglePacket.applyEffect(t, f, enable);
            syncToClient(t);
        }
        String who = t==req ? "" : " \u00a78[\u00a7f"+t.getName().getString()+"\u00a78]";
        ctx.getSource().sendSuccess(() -> Component.literal(
            "\u00a78[AT] \u00a7f"+f.displayName+" : "+(enable?"\u00a7aactive":"\u00a7cdesactive")+who), false);
        return 1;
    }

    private static int cmdTp(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer req = ctx.getSource().getPlayer(); if (req == null) return 0;
        String name = StringArgumentType.getString(ctx,"player");
        ServerPlayer t = ctx.getSource().getServer().getPlayerList().getPlayerByName(name);
        if (t == null) { ctx.getSource().sendFailure(Component.literal("\u00a7cIntrouvable : "+name)); return 0; }
        if (t == req)  { ctx.getSource().sendFailure(Component.literal("\u00a7cNe peut pas se TP a soi-meme.")); return 0; }
        req.teleportTo(t.serverLevel(), t.getX(), t.getY(), t.getZ(), t.getYRot(), t.getXRot());
        ctx.getSource().sendSuccess(() -> Component.literal(
            "\u00a78[AT] \u00a77TP -> \u00a7f"+name+"\u00a78 ("+
            (int)t.getX()+","+(int)t.getY()+","+(int)t.getZ()+")"), false);
        return 1;
    }

    private static int cmdInvSee(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer req = ctx.getSource().getPlayer(); if (req == null) return 0;
        String name = StringArgumentType.getString(ctx,"player");
        ServerPlayer t = ctx.getSource().getServer().getPlayerList().getPlayerByName(name);
        if (t == null) { ctx.getSource().sendFailure(Component.literal("\u00a7cIntrouvable : "+name)); return 0; }
        List<ItemStack> items = new ArrayList<>(41);
        for (int i=0;i<36;i++) items.add(t.getInventory().getItem(i).copy());
        for (int i=0;i<4; i++) items.add(t.getInventory().armor.get(i).copy());
        items.add(t.getInventory().offhand.get(0).copy());
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(()->req), new S2CInvSeePacket(name, items));
        ctx.getSource().sendSuccess(() -> Component.literal(
            "\u00a78[AT] \u00a77InvSee : \u00a7f"+name+"\u00a77 envoye."), false);
        return 1;
    }

    private static int cmdTopLuck(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer req = ctx.getSource().getPlayer();
        List<TopLuckTracker.Entry> board = TopLuckTracker.getLeaderboard();

        if (req == null) {
            // Console : texte
            ctx.getSource().sendSuccess(() -> Component.literal("\u00a78=== Top Luck ==="), false);
            for (int i = 0; i < Math.min(board.size(), 10); i++) {
                TopLuckTracker.Entry e = board.get(i);
                int rank = i+1;
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "\u00a78#"+rank+" \u00a7f"+e.name+" : "+
                    String.format("%.1f",e.ratio()*100)+"% ("+
                    e.totalOres()+"/"+e.totalHostRock()+" roche)"+(e.isSuspect()?" [SUSPECT]":"")), false);
            }
            return 1;
        }

        // Convertir en TopLuckEntry (DTO neutre) pour le GUI
        List<TopLuckEntry> guiEntries = new ArrayList<>(board.size());
        for (TopLuckTracker.Entry e : board) {
            // Ore groups tries
            List<Map.Entry<String,Integer>> sortedOres = e.sortedGroups();
            Map<String,Integer> oreMap = new LinkedHashMap<>();
            sortedOres.forEach(g -> oreMap.put(g.getKey(), g.getValue()));

            // All blocks tries, limites a MAX_DETAIL
            List<Map.Entry<String,Integer>> sortedBlocks = e.sortedAllBlocks();
            Map<String,Integer> blMap = new LinkedHashMap<>();
            sortedBlocks.stream().limit(TopLuckEntry.MAX_DETAIL)
                .forEach(g -> blMap.put(g.getKey(), g.getValue()));

            guiEntries.add(new TopLuckEntry(
                e.name, e.overworldRock, e.netherRock, e.endRock, oreMap, blMap));
        }

        NetworkHandler.CHANNEL.send(
            PacketDistributor.PLAYER.with(()->req),
            new S2CTopLuckPacket(guiEntries)
        );
        ctx.getSource().sendSuccess(() -> Component.literal(
            "\u00a78[AT] \u00a77Classement envoye ("+board.size()+" joueurs)."), false);
        return 1;
    }

    private static int cmdTopLuckReset(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx,"player");
        ServerPlayer t = ctx.getSource().getServer().getPlayerList().getPlayerByName(name);
        if (t==null) { ctx.getSource().sendFailure(Component.literal("\u00a7cIntrouvable : "+name)); return 0; }
        TopLuckTracker.reset(t.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal(
            "\u00a78[AT] \u00a77Stats de \u00a7f"+name+"\u00a77 remises a zero."), false);
        return 1;
    }

    private static int cmdCheck(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx,"player");
        ServerPlayer t = ctx.getSource().getServer().getPlayerList().getPlayerByName(name);
        if (t==null) { ctx.getSource().sendFailure(Component.literal("\u00a7cIntrouvable : "+name)); return 0; }
        var pos = t.blockPosition();
        ctx.getSource().sendSuccess(() -> Component.literal(
            "\u00a78=== \u00a7bCheck: \u00a7f"+name+" \u00a78===\n"
            +"\u00a77Mode : \u00a7f"+t.gameMode.getGameModeForPlayer().getName()+"\n"
            +"\u00a77Pos  : \u00a7f"+pos.getX()+", "+pos.getY()+", "+pos.getZ()+"\n"
            +"\u00a77Vol  : "+(t.getAbilities().flying?"\u00a7cOUI":"\u00a7aNON")+"\n"
            +"\u00a77Mode : "+(t.isCreative()?"\u00a7eCREATIF":t.isSpectator()?"\u00a7eSPECTATEUR":"\u00a7aNORMAL")+"\n"
            +"\u00a77Ping : \u00a7f"+t.latency+"ms"), false);
        return 1;
    }

    private static int cmdHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(
            "\u00a78=== \u00a7bAdminTools \u00a78===\n"
            +"\u00a7f/at status [joueur]           \u00a78- \u00a77Features\n"
            +"\u00a7f/at toggle <feature> [joueur]  \u00a78- \u00a77Basculer\n"
            +"\u00a7f/at on/off <feature> [joueur]  \u00a78- \u00a77Forcer\n"
            +"\u00a7f/at tp <joueur>                \u00a78- \u00a77Teleporter\n"
            +"\u00a7f/at invsee <joueur>             \u00a78- \u00a77Inventaire (GUI)\n"
            +"\u00a7f/at topluck [reset <joueur>]   \u00a78- \u00a77Classement (GUI)\n"
            +"\u00a7f/at check <joueur>              \u00a78- \u00a77Infos rapides\n"
            +"\u00a77Panel : \u00a7b[K]"), false);
        return 1;
    }

    private static ServerPlayer resolveTarget(CommandContext<CommandSourceStack> ctx, String name) {
        if (name == null) {
            ServerPlayer p = ctx.getSource().getPlayer();
            if (p == null) ctx.getSource().sendFailure(Component.literal("Joueur uniquement."));
            return p;
        }
        ServerPlayer t = ctx.getSource().getServer().getPlayerList().getPlayerByName(name);
        if (t == null) ctx.getSource().sendFailure(Component.literal("\u00a7cIntrouvable : "+name));
        return t;
    }

    private static AdminFeature parseFeature(String name, CommandContext<CommandSourceStack> ctx) {
        try { return AdminFeature.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("\u00a7cFeature inconnue : "+name)); return null;
        }
    }

    private static void syncToClient(ServerPlayer p) {
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),
            new S2CStatePacket(AdminToolsState.getFeatures(p.getUUID())));
    }
}
