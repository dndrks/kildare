KildareRS {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	init {
		SynthDef(\kildare_rs, {
			arg out = 0, stopGate = 1,
			delayAuxL, delayAuxR, delaySend,
			delayEnv, delayAtk, delayRel,
			feedbackAux,feedbackSend,
			velocity,
			carHz, carDetune,
			modHz, modAmp,
			modFollow, modNum, modDenum,
			carAtk, carRel, carCurve = -4, amp,
			pan, rampDepth, rampDec, amDepth, amHz,
			eqHz, eqAmp, bitRate, bitCount,
			sdAmp, sdRel, sdAtk, sdCurve = -4,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpCurve = -4, lpDepth,
			squishPitch, squishChunk;

			var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar, ampMod,
			mod_1,mod_2,feedAmp,feedAMP, sd_modHz,
			sd_car, sd_mod, sd_carEnv, sd_modEnv, sd_carRamp, sd_feedMod, sd_feedCar, sd_noise, sd_noiseEnv,
			sd_mix, filterEnv, delEnv, mainSendMix, delaySendMix;

			amp = amp*0.45;
			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			delaySend = delaySend.lag3(0.1);
			feedbackSend = feedbackSend.lag3(0.1);

			carHz = carHz * (2.pow(carDetune/12));
			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

			filterQ = LinLin.kr(filterQ,0,100,1.0,0.001);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

			feedAmp = modAmp.linlin(0, 127, 0, 3);
			feedAMP = modAmp.linlin(0, 127, 0, 4);

			carRamp = EnvGen.kr(Env([600, 0.000001], [rampDec], curve: \lin));
			carEnv = EnvGen.kr(Env.perc(carAtk, carRel, curve: carCurve),gate: stopGate);
			filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, curve: lpCurve),gate: stopGate);

			mod_2 = SinOscFB.ar(
				modHz*16,
				feedAmp,
				modAmp*10
			)* 1;

			mod_1 = SinOscFB.ar(
				modHz+mod_2,
				feedAmp,
				modAmp*10
			)* 1;

			car = SinOscFB.ar(carHz + (mod_1+mod_2) + (carRamp*rampDepth),feedAMP) * carEnv * amp;

			ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);

			car = (car+(LPF.ar(Impulse.ar(0.003),16000,1)*amp)) * ampMod;
			car = LPF.ar(car,12000,1);
			car = car.softclip;

			sd_modHz = carHz*2.52;
			sd_modEnv = EnvGen.kr(Env.perc(carAtk, carRel, curve: carCurve));
			sd_carRamp = EnvGen.kr(Env([1000, 0.000001], [rampDec], curve: \exp));
			sd_carEnv = EnvGen.kr(Env.perc(sdAtk, sdRel, curve: sdCurve),gate:stopGate);
			sd_feedMod = SinOsc.ar(modHz, mul:modAmp*100) * sd_modEnv;
			sd_feedCar = SinOsc.ar(carHz + sd_feedMod + (carRamp*rampDepth)) * sd_carEnv * (feedAmp*10);
			sd_mod = SinOsc.ar(modHz + sd_feedCar, mul:modAmp) * sd_modEnv;
			sd_car = SinOsc.ar(carHz + sd_mod + (carRamp*rampDepth)) * sd_carEnv * sdAmp;
			sd_mix = sd_car * ampMod;
			sd_mix = sd_mix.softclip;

			delEnv = Select.kr(delayEnv > 0, [delaySend, (delaySend * EnvGen.kr(Env.perc(delayAtk, delayRel),gate: stopGate))]);

			mainSendMix = (car + sd_mix);
			mainSendMix = Squiz.ar(in:mainSendMix, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			mainSendMix = Decimator.ar(mainSendMix,bitRate,bitCount,1.0);
			mainSendMix = BPeakEQ.ar(in:mainSendMix,freq:eqHz,rq:1,db:eqAmp,mul:1);
			mainSendMix = RLPF.ar(in:mainSendMix,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			mainSendMix = RHPF.ar(in:mainSendMix,freq:hpHz, rq: filterQ, mul:1);
			mainSendMix = Compander.ar(in:mainSendMix,control:mainSendMix, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			delaySendMix = mainSendMix;
			mainSendMix = Pan2.ar(mainSendMix,pan);
			mainSendMix = mainSendMix * amp * LinLin.kr(velocity,0,127,0.0,1.0);

			Out.ar(out, mainSendMix);
			Out.ar(delayAuxL, (delaySendMix * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(delayAuxR, (delaySendMix * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(feedbackAux, (mainSendMix * feedbackSend));

			FreeSelf.kr(Done.kr(sd_carEnv) * Done.kr(carEnv));

		}).send;
	}
}