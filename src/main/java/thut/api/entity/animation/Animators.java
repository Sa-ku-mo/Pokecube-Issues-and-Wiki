package thut.api.entity.animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nfunk.jep.JEP;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import thut.api.entity.IAnimated.IAnimationHolder;
import thut.api.entity.IAnimated.MolangVars;
import thut.api.maths.Vector3;
import thut.core.client.render.model.IExtendedModelPart;
import thut.core.client.render.model.parts.Part;
import thut.core.common.ThutCore;

public class Animators
{
    public static interface IAnimator
    {
        boolean animate(Animation animation, IAnimationHolder holder, IExtendedModelPart part, float partialTick,
                float limbSwing, int tick);

        default void setColours(int... rgba)
        {}

        int getLength();

        boolean hasLimbBased();

        void setLimbBased();

        void setHidden(boolean hidden);

        default boolean conflicts(IAnimator other)
        {
            return true;
        }
    }

    public static void fillJEPs(JEP[] jeps, String _funcs)
    {
        String[] funcs = _funcs.split(",");
        func:
        for (String s : funcs)
        {
            String[] args = s.split(":");
            int i;
            switch (args[0])
            {
            case ("x"):
                i = 0;
                break;
            case ("y"):
                i = 1;
                break;
            case ("z"):
                i = 2;
                break;
            case ("rx"):
                i = 0;
                break;
            case ("ry"):
                i = 1;
                break;
            case ("rz"):
                i = 2;
                break;
            case ("dx"):
                i = 0;
                break;
            case ("dy"):
                i = 1;
                break;
            case ("dz"):
                i = 2;
                break;
            default:
                ThutCore.LOGGER.error("Malformed function animation {}", s);
                continue func;
            }
            var func = args[1];
            jeps[i] = new JEP();
            jeps[i].addStandardFunctions();
            jeps[i].addStandardConstants();
            for (var entry : MolangVars.JEP_VARS.entrySet())
                if (func.contains(entry.getKey())) jeps[i].addVariable(entry.getKey(), entry.getValue());
            jeps[i].parseExpression(func);
        }
    }

    public static class KeyframeAnimator implements IAnimator
    {
        private static final AnimationComponent DEFAULTS = new AnimationComponent();

        public static enum CHANNEL
        {
            POS("position"), ROT("rotation"), SCALE("scale"), OPACITY("opacity");

            private final String name;

            private CHANNEL(String name)
            {
                this.name = name;
            }

            public String getName()
            {
                return name;
            }
        }

        private static record AnimChannel(String channel, List<AnimationComponent> components, int length)
        {
        };

        float[] dr = new float[3];
        float[] ds = new float[3];
        float[] dx = new float[3];

        public final List<AnimationComponent> components;
        public final List<AnimChannel> channels = new ArrayList<>();
        private final Set<CHANNEL> channelSet = new HashSet<>();
        private int length = -1;
        private boolean limbBased;
        private boolean hidden = false;

        public KeyframeAnimator(AnimationComponent component)
        {
            this(Lists.newArrayList(component), false);
        }

        public KeyframeAnimator(List<AnimationComponent> components)
        {
            this(components, false);
        }

        public KeyframeAnimator(List<AnimationComponent> components, boolean preComputed)
        {
            this.components = components;
            this.length = -1;
            this.limbBased = false;
            Map<String, List<AnimationComponent>> by_channel = new HashMap<>();
            for (final AnimationComponent component : components)
            {
                this.length = Math.max(this.length, component.startKey + component.length);
                this.limbBased = this.limbBased || component.limbBased;

                // Set anything close enough to 0 to 0. This fixes -0.0 != 0.0
                // in the below equals check
                for (int i = 0; i < 3; i++)
                {
                    if (Math.abs(component.posChange[i]) < 1e-5) component.posChange[i] = 0;
                    if (Math.abs(component.posOffset[i]) < 1e-5) component.posOffset[i] = 0;
                    if (Math.abs(component.rotChange[i]) < 1e-5) component.rotChange[i] = 0;
                    if (Math.abs(component.rotOffset[i]) < 1e-5) component.rotOffset[i] = 0;
                }

                boolean position_channel = !Arrays.equals(component.posChange, DEFAULTS.posChange);
                position_channel |= !Arrays.equals(component.posOffset, DEFAULTS.posOffset);
                position_channel |= !Arrays.equals(component._posFunctions, DEFAULTS._posFunctions);

                boolean rotation_channel = !Arrays.equals(component.rotChange, DEFAULTS.rotChange);
                rotation_channel |= !Arrays.equals(component.rotOffset, DEFAULTS.rotOffset);
                rotation_channel |= !Arrays.equals(component._rotFunctions, DEFAULTS._rotFunctions);

                boolean scale_channel = !Arrays.equals(component.scaleChange, DEFAULTS.scaleChange);
                scale_channel |= !Arrays.equals(component.scaleOffset, DEFAULTS.scaleOffset);
                scale_channel |= !Arrays.equals(component._scaleFunctions, DEFAULTS._scaleFunctions);

                boolean opac_channel = component._opacFunction != null;
                opac_channel |= component.opacityChange != DEFAULTS.opacityChange;
                opac_channel |= component.opacityOffset != DEFAULTS.opacityOffset;

                if (position_channel) component._valid_channels.add("position");
                if (rotation_channel) component._valid_channels.add("rotation");
                if (scale_channel) component._valid_channels.add("scale");
                if (opac_channel) component._valid_channels.add("opacity");

                component._foundNoJEP = Arrays.equals(component._posFunctions, DEFAULTS._posFunctions);
                component._foundNoJEP &= Arrays.equals(component._rotFunctions, DEFAULTS._rotFunctions);
                component._foundNoJEP &= Arrays.equals(component._scaleFunctions, DEFAULTS._scaleFunctions);

                for (String channel : component._valid_channels)
                {
                    var list = by_channel.computeIfAbsent(channel, c -> new ArrayList<>());
                    list.add(component);
                }
            }

            for (var channelEnum : CHANNEL.values())
            {
                String key = channelEnum.getName();
                var list = by_channel.get(key);
                if (list != null)
                {
                    int len = 1;
                    for (var comp : list) len = Math.max(len, comp.startKey + comp.length);
                    AnimChannel channel = new AnimChannel(key, list, len);
                    this.channels.add(channel);
                    this.channelSet.add(channelEnum);
                }
                else this.channels.add(null);
            }

            if (!preComputed) for (var entry : by_channel.entrySet())
            {
                var list = entry.getValue();
                AnimationComponent prev = list.get(0);
                boolean position = entry.getKey().equals("position");
                boolean rotation = entry.getKey().equals("rotation");

                for (int i = 1; i < list.size(); i++)
                {
                    AnimationComponent here = list.get(i);
                    for (int j = 0; j < 3 && (position || rotation); j++)
                    {
                        if (position) here.posOffset[j] += prev.posOffset[j] + prev.posChange[j];
                        if (rotation) here.rotOffset[j] += prev.rotOffset[j] + prev.rotChange[j];
                    }
                    prev = here;
                }
            }
        }

        private AnimationComponent getNext(float time1, float time2, boolean loops, AnimChannel animChannel)
        {
            var components = animChannel.components();
            if (components.isEmpty()) return null;
            int n = components.size();

            if (loops)
            {
                time1 %= animChannel.length();
                time2 %= animChannel.length();
            }

            // Now we run through the components per channel
            AnimationComponent component = components.get(n - 1);
            for (int i = 0; i < n - 1; i++)
            {
                var tmp = components.get(i + 1);
                final float time = component.limbBased ? time2 : time1;
                if (tmp.startKey > time)
                {
                    component = components.get(i);
                    break;
                }
            }
            return component;
        }

        @Override
        public boolean animate(Animation animation, IAnimationHolder holder, IExtendedModelPart part, float partialTick,
                float limbSwing, int tick)
        {
            if (this.hidden) return false;

            boolean animated = false;
            final Vector3 temp = animation._shift.clear();

            float px = 0, py = 0, pz = 0;
            float rx = 0, ry = 0, rz = 0;
            float sx = 1, sy = 1, sz = 1;
            float alpha_scale = 1.0f;
            float time1;
            float time2;
            MolangVars molangs = holder.getMolangVars();

            time1 = (float) molangs.getAnimTime();
            time2 = (float) molangs.l;

            int aniTick = (int) Math.ceil(time1);

            boolean wasHidden = part.isHidden();

//            molangs.t = time1 = 1.9f * 20f;

            // First clear these
            dr[0] = dr[1] = dr[2] = 0;
            dx[0] = dx[1] = dx[2] = 0;
            ds[0] = ds[1] = ds[2] = 1;

//            System.out.println(animation.loops);

            // Marker for if any were hidden
            boolean any_hidden = false;

            boolean limb = part instanceof Part p && p.isOverridenLimb;

            // Now we run through the components per channel

            CHANNEL channel = CHANNEL.ROT;
            // Rotation set
            rots:
            {
                var animChannel = channels.get(channel.ordinal());
                if (animChannel == null) break rots;
                AnimationComponent component = getNext(time1, time2, animation.loops, animChannel);
                if (component == null) break rots;
                animated = true;

                float t1 = time1, t2 = time2;
                if (animation.loops)
                {
                    int l = animChannel.length();
                    if (l > 1)
                    {
                        t1 = time1 % l;
                        t2 = time2 % l;
                    }
                }
                else
                {
                    t1 = Math.min(time1, animChannel.length());
                    t2 = Math.min(time2, animChannel.length());
                }
                float time = component.limbBased || limb ? t2 : t1;
                aniTick = Math.max(aniTick, (int) Math.ceil(time));

                any_hidden |= component.hidden;

                // Start by checking JEP components to the animation
                for (int i = 0; i < 3; i++) if (component._rotFunctions[i] != null)
                {
                    molangs.updateJEP(component._rotFunctions[i], t1, t2);
                    dr[i] = (float) component._rotFunctions[i].getValue() * component._rotFuncScale[i];
                }

                float componentTimer = time - component.startKey;
                if (componentTimer > component.length) componentTimer = component.length;
                final int length = component.length == 0 ? 1 : component.length;
                final float ratio = componentTimer / length;

                rx += component.rotChange[0] * ratio + component.rotOffset[0];
                ry += component.rotChange[1] * ratio + component.rotOffset[1];
                rz += component.rotChange[2] * ratio + component.rotOffset[2];
            }

            channel = CHANNEL.POS;
            // Position set
            pos:
            {
                var animChannel = channels.get(channel.ordinal());
                if (animChannel == null) break pos;
                AnimationComponent component = getNext(time1, time2, animation.loops, animChannel);
                if (component == null) break pos;
                animated = true;

                float t1 = time1, t2 = time2;
                if (animation.loops)
                {
                    int l = animChannel.length();
                    if (l > 1)
                    {
                        t1 = time1 % l;
                        t2 = time2 % l;
                    }
                }
                else
                {
                    t1 = Math.min(time1, animChannel.length());
                    t2 = Math.min(time2, animChannel.length());
                }
                float time = component.limbBased || limb ? t2 : t1;
                aniTick = Math.max(aniTick, (int) Math.ceil(time));

                any_hidden |= component.hidden;

                // Start by checking JEP components to the animation
                for (int i = 0; i < 3; i++) if (component._posFunctions[i] != null)
                {
                    molangs.updateJEP(component._posFunctions[i], t1, t2);
                    dx[i] = (float) component._posFunctions[i].getValue() * component._posFuncScale[i];
                }

                float componentTimer = time - component.startKey;
                if (componentTimer > component.length) componentTimer = component.length;
                final int length = component.length == 0 ? 1 : component.length;
                final float ratio = componentTimer / length;

                px += component.posChange[0] * ratio + component.posOffset[0];
                py += component.posChange[1] * ratio + component.posOffset[1];
                pz += component.posChange[2] * ratio + component.posOffset[2];
            }

            channel = CHANNEL.SCALE;
            // scale set
            scales:
            {
                var animChannel = channels.get(channel.ordinal());
                if (animChannel == null) break scales;
                AnimationComponent component = getNext(time1, time2, animation.loops, animChannel);
                if (component == null) break scales;
                animated = true;

                float t1 = time1, t2 = time2;
                if (animation.loops)
                {
                    int l = animChannel.length();
                    if (l > 1)
                    {
                        t1 = time1 % l;
                        t2 = time2 % l;
                    }
                }
                else
                {
                    t1 = Math.min(time1, animChannel.length());
                    t2 = Math.min(time2, animChannel.length());
                }
                float time = component.limbBased || limb ? t2 : t1;
                aniTick = Math.max(aniTick, (int) Math.ceil(time));

                any_hidden |= component.hidden;

                // Start by checking JEP components to the animation
                for (int i = 0; i < 3; i++) if (component._scaleFunctions[i] != null)
                {
                    molangs.updateJEP(component._scaleFunctions[i], t1, t2);
                    ds[i] = (float) component._scaleFunctions[i].getValue() * component._scaleFuncScale[i];
                }

                float componentTimer = time - component.startKey;
                if (componentTimer > component.length) componentTimer = component.length;
                final int length = component.length == 0 ? 1 : component.length;
                final float ratio = componentTimer / length;

                sx *= component.scaleChange[0] * ratio + component.scaleOffset[0];
                sy *= component.scaleChange[1] * ratio + component.scaleOffset[1];
                sz *= component.scaleChange[2] * ratio + component.scaleOffset[2];
            }

            channel = CHANNEL.OPACITY;
            // scale set
            opacity:
            {
                var animChannel = channels.get(channel.ordinal());
                if (animChannel == null) break opacity;
                AnimationComponent component = getNext(time1, time2, animation.loops, animChannel);
                if (component == null) break opacity;
                animated = true;

                float t1 = time1, t2 = time2;
                if (animation.loops)
                {
                    int l = animChannel.length();
                    if (l > 1)
                    {
                        t1 = time1 % l;
                        t2 = time2 % l;
                    }
                }
                else
                {
                    t1 = Math.min(time1, animChannel.length());
                    t2 = Math.min(time2, animChannel.length());
                }
                float time = component.limbBased || limb ? t2 : t1;
                aniTick = Math.max(aniTick, (int) Math.ceil(time));

                any_hidden |= component.hidden;
                if (component._opacFunction != null)
                {
                    molangs.updateJEP(component._opacFunction, t1, t2);
                    alpha_scale *= component._opacFunction.getValue();
                }

                float componentTimer = time - component.startKey;
                if (componentTimer > component.length) componentTimer = component.length;
                final int length = component.length == 0 ? 1 : component.length;
                final float ratio = componentTimer / length;

                alpha_scale *= component.opacityOffset + ratio * component.opacityChange;
            }

//            any_hidden = part.getName().contains("hair");

            // Apply hidden like this so last hidden state is kept
            if (wasHidden != any_hidden) part.setHidden(any_hidden);
            part.setOpacityScale(alpha_scale);
            holder.setStep(animation, aniTick);
            if (animated)
            {
                temp.set(px, py, pz);

                rx += dr[0];
                ry += dr[1];
                rz += dr[2];

                temp.addTo(dx[0], dx[1], dx[2]);

                sx *= ds[0];
                sy *= ds[1];
                sz *= ds[2];
                part.setPreTranslations(temp);
                part.setPreScale(temp.set(sx, sy, sz));
                part.setAnimAngles(rx, ry, rz);
            }

            return animated;
        }

        @Override
        public int getLength()
        {
            return this.length;
        }

        @Override
        public boolean hasLimbBased()
        {
            return limbBased;
        }

        @Override
        public void setLimbBased()
        {
            limbBased = true;
        }

        @Override
        public void setHidden(boolean hidden)
        {
            this.hidden = hidden;
            for (final AnimationComponent component : components) component.hidden = hidden;
        }

        @Override
        public boolean conflicts(IAnimator other)
        {
            if (other instanceof KeyframeAnimator anim)
            {
                Set<CHANNEL> our_channels = this.channelSet;
                Set<CHANNEL> other_channels = anim.channelSet;
                return Sets.intersection(our_channels, other_channels).isEmpty();
            }
            return IAnimator.super.conflicts(other);
        }
    }
}
