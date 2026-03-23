package com.admintools;

import com.admintools.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * AdminTools -- Mod principal Forge 1.20.1
 *
 * Fonctionnalites :
 *   * Ore X-Ray ESP (client) -- minerais visibles a travers les blocs
 *   * Entity ESP   (client) -- joueurs/mobs visibles a travers les murs
 *   * Vanish        (serveur) -- admin invisible pour les non-admins
 *   * Alertes anti-cheat     -- notifications en temps reel
 *   * Detection Fly           -- surveillance des vols illegaux
 *   * Detection NoClip        -- detection de passage dans les blocs
 *   * Detection X-Ray         -- analyse des taux de minage de minerais
 *   * Panel admin             -- touche K ou /at
 */
@Mod(AdminToolsMod.MOD_ID)
public class AdminToolsMod {

    public static final String MOD_ID = "admintools";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public AdminToolsMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);

        // Enregistrement de tous les event listeners (FORGE bus)
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[AdminTools] Mod charge. Forge 1.20.1");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.init();
            LOGGER.info("[AdminTools] Canal reseau initialise.");
        });
    }
}
