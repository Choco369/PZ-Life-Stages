package pz.playablechildren;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import me.zed_0xff.zombie_buddy.Patch;
import me.zed_0xff.zombie_buddy.Patch.OnEnter;
import me.zed_0xff.zombie_buddy.Patch.OnExit;
import me.zed_0xff.zombie_buddy.Patch.This;
import zombie.GameTime;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.IsoPlayer;
import zombie.inventory.types.Radio;
import zombie.network.GameServer;
import zombie.radio.devices.DeviceData;
import zombie.radio.devices.WaveSignalDevice;
import zombie.radio.media.MediaData;

/** Plays the supplied voice recording only while its CD is playing. */
public final class PatchDMVAudio {
    private static final String MEDIA_ID = "PZLS-DMV-AUDIO-1";
    private static final String SOUND = "PZLSDMVAudiobook";
    private static final Map<DeviceData, Session> sessions = new WeakHashMap<>();
    private static final Map<DeviceData, Boolean> stopped = new WeakHashMap<>();

    private PatchDMVAudio() {}

    private static final class Session {
        final DMVPlaybackSession playback = new DMVPlaybackSession(System.nanoTime() / 1_000_000L);
        final IdentityHashMap<IsoPlayer, Boolean> listeners = new IdentityHashMap<>();

        Session(DeviceData device) {
            for (IsoPlayer player : IsoPlayer.players) {
                if (canHear(device, player)) listeners.put(player, Boolean.TRUE);
            }
        }

        void updateListeners(DeviceData device) {
            Iterator<IsoPlayer> iterator = listeners.keySet().iterator();
            while (iterator.hasNext()) {
                if (!canHear(device, iterator.next())) iterator.remove();
            }
        }
    }

    private static boolean isOurCD(DeviceData device) {
        if (device == null || !device.hasMedia()) return false;
        MediaData media = device.getMediaData();
        return media != null && MEDIA_ID.equals(media.getId());
    }

    private static boolean isActive(DeviceData device) {
        return isOurCD(device) && device.isPlayingMedia()
            && device.getIsTurnedOn() && device.getDeviceVolume() > 0.0f;
    }

    private static float gameSpeed() {
        return GameTime.getInstance().getTrueMultiplier();
    }

    private static boolean canHear(DeviceData device, IsoPlayer player) {
        if (player == null || !player.isLocalPlayer()) return false;
        WaveSignalDevice parent = device.getParent();
        if (parent == null) return false;
        if (device.isInventoryDevice() && parent instanceof Radio radio) {
            return radio.getEquipParent() == player
                || radio.getContainer() != null && radio.getContainer().getParent() == player;
        }
        if (Math.abs(parent.getZ() - player.getZ()) >= 1.0f) return false;
        float dx = parent.getX() - player.getX();
        float dy = parent.getY() - player.getY();
        int range = device.getDeviceVolumeRange();
        return range > 0 && dx * dx + dy * dy <= range * range;
    }

    private static void grantToListeners(DeviceData device, Session session) {
        for (IsoPlayer player : session.listeners.keySet()) {
            if (canHear(device, player)) {
                player.getModData().rawset("PZLSDMVAudioFullPlayback", Boolean.TRUE);
            }
        }
    }

    // ZombieBuddy inlines the advice into DeviceData: every called bridge must be public.
    public static void cancel(DeviceData device, boolean suppressRestart) {
        Session removed = sessions.remove(device);
        if (removed != null) {
            BaseSoundEmitter emitter = device.getEmitter();
            if (emitter != null) emitter.stopSoundByName(SOUND);
        }
        if (suppressRestart && isOurCD(device)) stopped.put(device, Boolean.TRUE);
    }

    /** Vanilla may stop the CD on the same update that our recording finishes. */
    public static void finishOnStop(DeviceData device) {
        if (GameServer.server) return;
        Session session = sessions.get(device);
        if (session != null) {
            session.updateListeners(device);
            BaseSoundEmitter emitter = device.getEmitter();
            boolean soundPlaying = emitter != null && emitter.isPlaying(SOUND);
            DMVPlaybackSession.Status status = session.playback.observe(
                System.nanoTime() / 1_000_000L, soundPlaying, gameSpeed()
            );
            if (status == DMVPlaybackSession.Status.COMPLETE) {
                grantToListeners(device, session);
            }
        }
        cancel(device, true);
    }

    public static void beforeUpdate(DeviceData device) {
        if (GameServer.server) return;
        Session session = sessions.get(device);
        if (session == null || !isActive(device)) return;
        int lineIndex = ((Number)ReflectionAccess.tryGet(device, "mediaLineIndex", -1)).intValue();
        int lineCount = device.getMediaData().getLineCount();
        boolean atFinalLine = lineCount > 0 && lineIndex >= lineCount - 1;
        session.updateListeners(device);
        BaseSoundEmitter emitter = device.getEmitter();
        float speed = gameSpeed();
        boolean soundPlaying = emitter != null && emitter.isPlaying(SOUND);
        // Vanilla advances media at game speed; fast-forward may mute FMOD audio.
        boolean playingForCourse = session.playback.playsForCourse(soundPlaying, speed);
        DMVPlaybackSession.Status status =
            session.playback.observe(System.nanoTime() / 1_000_000L, playingForCourse, speed);
        if (status == DMVPlaybackSession.Status.COMPLETE) {
            grantToListeners(device, session);
            if (speed > 1.01f && soundPlaying && emitter != null) {
                emitter.stopSoundByName(SOUND);
            }
            if (atFinalLine) ReflectionAccess.trySet(device, "lineCounter", 0.0f);
            return;
        }
        if (status == DMVPlaybackSession.Status.INTERRUPTED) {
            cancel(device, true);
            if (atFinalLine) ReflectionAccess.trySet(device, "lineCounter", 0.0f);
            return;
        }
        if (session.playback.shouldHoldFinalLine(atFinalLine, playingForCourse)) {
            // Delay the final known-media line (and vanilla's check) until the voice ends.
            ReflectionAccess.trySet(device, "lineCounter", Float.MAX_VALUE);
        }
    }

    public static void afterUpdate(DeviceData device) {
        if (GameServer.server) return;
        if (!device.isPlayingMedia()) stopped.remove(device);
        if (!isActive(device)) {
            cancel(device, false);
            return;
        }
        if (stopped.containsKey(device) || sessions.containsKey(device)) return;
        Session session = new Session(device);
        sessions.put(device, session);
        device.playSoundLocal(SOUND, true);
    }

    @Patch(className = "zombie.radio.devices.DeviceData",
           methodName = "updateMediaPlaying", strictMatch = true)
    public static final class MediaUpdate {
        @OnEnter
        public static void enter(@This DeviceData device) {
            beforeUpdate(device);
        }

        @OnExit
        public static void exit(@This DeviceData device) {
            afterUpdate(device);
        }
    }

    @Patch(className = "zombie.radio.devices.DeviceData",
           methodName = "StopPlayMedia", strictMatch = true)
    public static final class MediaStop {
        @OnExit
        public static void exit(@This DeviceData device) {
            finishOnStop(device);
        }
    }
}
