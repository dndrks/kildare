KildareRS {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	init {
		SynthDef(\kildare_rs, {
			arg out = 0, stopGate = 1,
			delayAuxL, delayAuxR, delaySend,
			delayAtk, delayRel,
			reverbAux,reverbSend,
			carHz, carDetune,
			modHz, modAmp,
			modFollow, modNum, modDenum,
			carAtk, carRel, amp,
			pan, rampDepth, rampDec, amDepth, amHz,
			eqHz, eqAmp, bitRate, bitCount,
			sdAmp, sdRel, sdAtk,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpDepth,
			squishPitch, squishChunk;

			var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar, ampMod,
			mod_1,mod_2,feedAmp,feedAMP, sd_modHz,
			sd_car, sd_mod, sd_carEnv, sd_modEnv, sd_carRamp, sd_feedMod, sd_feedCar, sd_noise, sd_noiseEnv,
			sd_mix, filterEnv, delayEnv, mainSendCar, mainSendSnare;

			amp = amp*0.45;
			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			delaySend = delaySend.lag3(0.1);
			reverbSend = reverbSend.lag3(0.1);

			carHz = carHz * (2.pow(carDetune/12));
			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

			filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

			feedAmp = modAmp.linlin(0, 127, 0, 3);
			feedAMP = modAmp.linlin(0, 127, 0, 4);

			carRamp = EnvGen.kr(Env([600, 0.000001], [rampDec], curve: \lin));
			carEnv = EnvGen.kr(Env.perc(carAtk, carRel),gate: stopGate);
			filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);

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
			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);
			car = LPF.ar(car,12000,1);

			sd_modHz = carHz*2.52;
			sd_modEnv = EnvGen.kr(Env.perc(carAtk, carRel));
			sd_carRamp = EnvGen.kr(Env([1000, 0.000001], [rampDec], curve: \exp));
			sd_carEnv = EnvGen.kr(Env.perc(sdAtk, sdRel),gate:stopGate);
			sd_feedMod = SinOsc.ar(modHz, mul:modAmp*100) * sd_modEnv;
			sd_feedCar = SinOsc.ar(carHz + sd_feedMod + (carRamp*rampDepth)) * sd_carEnv * (feedAmp*10);
			sd_mod = SinOsc.ar(modHz + sd_feedCar, mul:modAmp) * sd_modEnv;
			sd_car = SinOsc.ar(carHz + sd_mod + (carRamp*rampDepth)) * sd_carEnv * sdAmp;
			sd_car = sd_car * ampMod;
			sd_mix = Squiz.ar(in:sd_car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			sd_mix = Decimator.ar(sd_mix,bitRate,bitCount,1.0);
			sd_mix = BPeakEQ.ar(in:sd_mix,freq:eqHz,rq:1,db:eqAmp,mul:1);
			sd_mix = RLPF.ar(in:sd_mix,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			sd_mix = RHPF.ar(in:sd_mix,freq:hpHz, rq: filterQ, mul:1);

			car = car.softclip;
			car = Compander.ar(in:car,control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSendCar = Pan2.ar(car,pan);
			mainSendCar = mainSendCar * amp;

			sd_mix = sd_mix.softclip;
			sd_mix = Compander.ar(in:sd_mix,control:sd_mix, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSendSnare = Pan2.ar(sd_mix,pan);
			mainSendSnare = mainSendSnare * amp;

			delayEnv = (delaySend * EnvGen.kr(Env.perc(delayAtk, delayRel, 1),gate: stopGate));

			Out.ar(out, mainSendCar);
			Out.ar(delayAuxL, (car * amp * delayEnv));
			Out.ar(delayAuxR, (car * amp * delayEnv));
			Out.ar(reverbAux, (mainSendCar * reverbSend));

			Out.ar(out, mainSendSnare);
			Out.ar(delayAuxL, (sd_mix * amp * delayEnv));
			Out.ar(delayAuxR, (sd_mix * amp * delayEnv));
			Out.ar(reverbAux, (mainSendSnare * reverbSend));

			FreeSelf.kr(Done.kr(sd_carEnv) * Done.kr(carEnv));

		}).send;
	}
}