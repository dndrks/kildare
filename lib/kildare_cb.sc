KildareCB {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	init {
		SynthDef(\kildare_cb, {
			arg out = 0, t_gate = 0,
			delayAuxL, delayAuxR, delaySend,
			delayEnv, delayAtk, delayRel,
			feedbackAux,feedbackSend,
			velocity = 127,
			amp, carHz, carDetune,
			modHz, modAmp, modAtk, modRel, modCurve = -4, feedAmp,
			modFollow, modNum, modDenum,
			carAtk, carRel, carCurve = -4,
			snap,
			pan, rampDepth, rampDec, amDepth, amHz,
			eqHz, eqAmp, bitRate, bitCount,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpCurve = -4, lpDepth,
			squishPitch, squishChunk;

			var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar, ampMod,
			voice_1, voice_2, filterEnv, delEnv, mainSend;

			amp = amp*0.6;
			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			delaySend = delaySend.lag3(0.1);
			feedbackSend = feedbackSend.lag3(0.1);

			carHz = carHz * (2.pow(carDetune/12));
			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

			filterQ = LinLin.kr(filterQ,0,100,1.0,0.001);
			feedAmp = LinLin.kr(feedAmp,0.0,1.0,1.0,3.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);
			snap = LinLin.kr(snap,0.0,1.0,0.0,10.0);

			modEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.01,modAtk,modRel], curve: [modCurve,modCurve*(-1)]),
				gate: t_gate
			);
			carRamp = EnvGen.kr(
				Env([600,600, 0.000001], [0,rampDec], curve: \lin),
				gate: t_gate
			);
			carEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.01,carAtk,carRel], curve: [carCurve,carCurve*(-1)]),
				gate: t_gate
			);
			filterEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.01,lpAtk,lpRel], curve: [lpCurve,lpCurve*(-1)]),
				gate: t_gate
			);

			voice_1 = LFPulse.ar((carHz) + (carRamp*rampDepth)) * carEnv * amp;
			voice_2 = SinOscFB.ar((carHz*1.5085)+ (carRamp*rampDepth),feedAmp) * carEnv * amp;
			ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);
			voice_1 = (voice_1+(LPF.ar(Impulse.ar(0.003),16000,1)*snap)) * ampMod;
			voice_1 = (voice_1*0.33)+(voice_2*0.33);
			voice_1 = Squiz.ar(in:voice_1, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			voice_1 = Decimator.ar(voice_1,bitRate,bitCount,1.0);
			voice_1 = BPeakEQ.ar(in:voice_1,freq:eqHz,rq:1,db:eqAmp,mul:1);
			voice_1 = RLPF.ar(in:voice_1,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			voice_1 = RHPF.ar(in:voice_1,freq:hpHz, rq: filterQ, mul:1);

			voice_1 = Compander.ar(in:voice_1,control:voice_1, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSend = Pan2.ar(voice_1,pan);
			mainSend = mainSend * (amp * LinLin.kr(velocity,0,127,0.0,1.0));

			delEnv = Select.kr(
				delayEnv > 0, [
					delaySend,
					delaySend * EnvGen.kr(
						envelope: Env.new([0,0,1,0], times: [0.01,delayAtk,delayRel]),
						gate: t_gate
					)
				]
			);

			Out.ar(out, mainSend);
			Out.ar(delayAuxL, (voice_1 * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(delayAuxR, (voice_1 * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(feedbackAux, (mainSend * feedbackSend));

		}).send;
	}
}