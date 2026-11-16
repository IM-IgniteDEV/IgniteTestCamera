# IgniteTestCamera

Client-side cinematic camera for a Fabric Minecraft client, driven by IgniteTestTool over a
localhost socket. It controls the render camera directly (position, yaw, pitch, roll, FOV) at the
client's real framerate. No protocol packets are sent, so it cannot crash the client the way packet
injection does and it does not touch gameplay - it is a visual admin camera for a server owner
filming their own server.

## Target

- Minecraft 1.21.11, Fabric loader 0.20.1
- Yarn mappings 1.21.11+build.6, Fabric API 0.141.6+1.21.11
- fabric-loom 1.14.10, Gradle 9.3, Java 21
- Coexists with Sodium + Iris (Fabulously Optimized). Mixins are minimal and defensive.

## Build

The project path contains a non-ASCII character that breaks Gradle, so build through an ASCII
junction (same approach IgniteTestTool uses with `D:\itt_build`):

```
cmd /c mklink /J D:\itcam_build "D:\Workplace\Ignite Series\⇒ MAINTAINED\IgniteTestCamera"
Set-Location D:\itcam_build
.\gradlew.bat build
```

Output jar: `build\libs\ignitetestcamera-1.0.0.jar` (drop it in the client `mods` folder next to
Sodium, Iris and Fabric API).

## Control channel

A TCP server bound to `127.0.0.1` only. One JSON object per line in, one JSON object per line out.
Default port `8767`. Override with `config/ignitetestcamera.properties` (`port=...`) or the system
property `-Dignitetestcamera.port=...`.

Mutating commands are applied on the client thread; the socket never blocks the render thread.

### Commands

- `{"cmd":"ping"}` -> `{"ok":true}`
- `{"cmd":"freecam","on":true}` - detach the camera from the player. `on:false` returns to vanilla.
- `{"cmd":"pose","x":..,"y":..,"z":..,"yaw":..,"pitch":..,"roll":..,"fov":..}` - jump to a pose now
  (implies freecam on). `roll` and `fov` are optional; omitting `fov` leaves the game FOV alone.
- `{"cmd":"path","ms":4000,"loop":false,"keyframes":[ ... ]}` - play a keyframed move over `ms`
  milliseconds (implies freecam on). Each keyframe:
  `{"t":0,"x":..,"y":..,"z":..,"yaw":..,"pitch":..,"roll":..,"fov":..,"ease":"smooth"}`.
  `t` (ms from start) is optional; without it keyframes are spread evenly across `ms`. `ease` is one
  of `linear`, `in`, `out`, `inout`, `smooth` (default). Needs at least 2 keyframes.
- `{"cmd":"fov","value":30}` - live FOV override for dolly-zoom ramps. `value<=0` clears it.
- `{"cmd":"hud","hide":true,"nametags":true}` - hide HUD, crosshair and hand (vanilla `hudHidden`)
  and, separately, entity nametags. `hide:false` restores. `nametags` defaults to `hide`;
  `hide:true,nametags:false` keeps name tags drawn even though vanilla skips them with the HUD hidden.
- `{"cmd":"status"}` -> `{"ok":true,"active":..,"playing":..,"hudHidden":..,"nametagsHidden":..,
  "fovOverride":..,"pose":{"x":..,"y":..,"z":..,"yaw":..,"pitch":..,"roll":..,"fov":..}}`.
  `pose` is the last pose actually applied to the camera this frame.

### Interpolation

The mod runs the path itself. Each frame, `CameraController.sample()` reads the wall-clock elapsed
time since the path started (`System.nanoTime`), finds the active segment and interpolates:

- position: Catmull-Rom spline through the keyframes (endpoints duplicated for the tangents)
- yaw: shortest-angle lerp; pitch, roll, fov: lerp
- the segment's start-keyframe `ease` warps the segment parameter

Because sampling is time-based and happens inside the camera update every rendered frame, motion is
smooth at the client's true framerate rather than stepped at 20 Hz.

## Mixins

- `CameraMixin` -> `Camera.update(World, Entity, boolean, boolean, float)` at TAIL: overrides the
  camera position and rotation with the sampled pose, and folds roll into the camera rotation
  quaternion (`getRotation().rotateZ`).
- `GameRendererMixin` -> `GameRenderer.getFov(Camera, float, boolean)` at RETURN: replaces the
  returned FOV with the commanded value when an override is active.
- `EntityRendererMixin` -> `EntityRenderer.hasLabel(Entity, double)` at HEAD (require 0): returns
  false while nametags are hidden.
- `LivingEntityRendererMixin` -> `LivingEntityRenderer.hasLabel(LivingEntity, double)`, the label check for
  players and mobs: HEAD returns false while nametags are hidden, and the `MinecraftClient.isHudEnabled()`
  call is redirected to true while the mod hides the HUD, so name tags can stay on.

HUD/hand/crosshair hiding uses the vanilla `GameOptions.hudHidden` flag rather than a mixin.

## Iris DOF (stretch) - skipped

Iris does not expose a stable public API for setting DOF focus distance or arbitrary shader
uniforms from another mod. Doing it would require reflecting into Iris internals, which risks
destabilizing Sodium/Iris rendering, so it was left out per the brief.

## Needs a live check

- Roll: whether the renderer consumes `Camera.getRotation()` for the view matrix (it should on
  1.21.11). If the horizon does not tilt, apply the Z roll in the world-render matrix instead.
- Nametag hide and show with the HUD hidden: `LivingEntityRenderer.hasLabel` is the gate; verify in game.
- That `hudHidden` still hides the first-person hand on 1.21.11.
