package com.jelly.farmhelper.world;
import com.jelly.farmhelper.FarmHelper;
import com.jelly.farmhelper.features.FailsafeNew;
import com.jelly.farmhelper.features.PetSwapper;
import com.jelly.farmhelper.features.Scheduler;
import com.jelly.farmhelper.hud.DebugHUD;
import com.jelly.farmhelper.macros.MacroHandler;
import com.jelly.farmhelper.utils.Clock;
import com.jelly.farmhelper.utils.LocationUtils;
import com.jelly.farmhelper.utils.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class JacobsContestHandler {
    public static boolean jacobsContestTriggered = false;
    private static final Clock jacobsContestDelay = new Clock();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (event.phase == TickEvent.Phase.END) {
            if (!jacobsContestTriggered && (!jacobsContestDelay.isScheduled() || jacobsContestDelay.passed()) && GameState.inJacobContest()) {
                if (FarmHelper.config.pauseSchedulerDuringJacobsContest) {
                    LogUtils.debugLog("Jacob's contest start detected, pausing scheduler");
                    Scheduler.pause();
                }
                jacobsContestTriggered = true;
                DebugHUD.jacobsContestTriggered = true;
                jacobsContestDelay.reset();
                jacobsContestDelay.schedule(10000L);
                if (FarmHelper.config.enablePetSwapper) {
                    if (LocationUtils.currentIsland != LocationUtils.Island.GARDEN) return;
                    if (PetSwapper.isEnabled()) return;
                    if (PetSwapper.currentState != PetSwapper.State.NONE) return;
                    if (FailsafeNew.emergency) return;
                    if (!MacroHandler.isMacroing) return;
                    if (MacroHandler.currentMacro == null || !MacroHandler.currentMacro.enabled) return;
                    if (GameState.inJacobContest() && !PetSwapper.hasPetChangedDuringThisContest) {
                        LogUtils.debugLog("Jacob's contest start detected, swapping pet");
                        PetSwapper.startMacro(false);
                        PetSwapper.hasPetChangedDuringThisContest = true;
                    }
                }
            }
        }
    }

    Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChatMessage(ClientChatReceivedEvent event) {
//        if (event.type != 0) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        String msg = StringUtils.stripControlCodes(event.message.getUnformattedText().toLowerCase());
        if (msg.contains("the farming contest is over")) {
            jacobsContestTriggered = false;
            DebugHUD.jacobsContestTriggered = false;
            jacobsContestDelay.reset();
            jacobsContestDelay.schedule(10000L);
            if (!Scheduler.isFarming())
                MacroHandler.disableCurrentMacro(true);
            if (FarmHelper.config.enablePetSwapper) {
                if (PetSwapper.isEnabled()) return;
                if (PetSwapper.currentState != PetSwapper.State.NONE) return;
                if (FailsafeNew.emergency) return;
                if (!MacroHandler.isMacroing) return;
                if (MacroHandler.currentMacro == null || !MacroHandler.currentMacro.enabled) return;
                if (!PetSwapper.hasPetChangedDuringThisContest) return;
                LogUtils.debugLog("Jacob's contest end detected, swapping pet");
                PetSwapper.startMacro(true);
                PetSwapper.hasPetChangedDuringThisContest = false;
            } else {
                LogUtils.debugLog("Jacob's contest end detected, resuming scheduler");
                Scheduler.resume();
            }
        }
    }
}
