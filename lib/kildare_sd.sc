KildareSD {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	init {
		SynthDef(\kildare_sd, {
			arg out = 0, t_gate = 0,
			delayAuxL, delayAuxR, delaySend,
			delayEnv, delayAtk, delayRel,
			feedbackAux,feedbackSend,
			velocity = 127,
			carHz, carHzThird, carHzSeventh,
			carDetune, carAtk, carRel, carCurve = -4,
			modHz, modAmp, modAtk, modRel, modCurve = -4, feedAmp,
			modFollow, modNum, modDenum,
			amp, pan,
			rampDepth, rampDec, noiseAmp,
			noiseAtk, noiseRel, noiseCurve = -4, bitRate, bitCount,
			eqHz,eqAmp,
			squishPitch, squishChunk,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpCurve = -4, lpDepth,
			amDepth, amHz;

			var car, carThird, carSeventh,
			modHzThird, modHzSeventh,
			mod_1, mod_2, mod_3,
			carEnv, modEnv, carRamp, feedMod, feedCar,
			noise, noiseEnv, mix, ampMod, filterEnv, delEnv, mainSendCar, mainSendNoise;

			amp = amp;
			noiseAmp = noiseAmp/2;
			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			delaySend = delaySend.lag3(0.1);
			feedbackSend = feedbackSend.lag3(0.1);

			carHz = (carHz * (1 - modFollow)) + (carHz * modFollow * modNum);
			carHzThird = (carHzThird * (1 - modFollow)) + (carHzThird * modFollow * modNum);
			carHzSeventh = (carHzSeventh * (1 - modFollow)) + (carHzSeventh * modFollow * modNum);

			carHz = carHz * (2.pow(carDetune/12));
			carHzThird = carHzThird * (2.pow(carDetune/12));
			carHzSeventh = carHzSeventh * (2.pow(carDetune/12));

			modHz = (modHz * (1 - modFollow)) + (carHz * modFollow * modDenum);
			modHzThird = (modHz * (1 - modFollow)) + (carHzThird * modFollow * modDenum);
			modHzSeventh = (modHz * (1 - modFollow)) + (carHzSeventh * modFollow * modDenum);

			filterQ = LinLin.kr(filterQ,0,100,1.0,0.001);
			modEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.01,modAtk,modRel], curve: [modCurve,modCurve*(-1)]),
				gate: t_gate
			);
			filterEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.01,lpAtk,lpRel], curve: [lpCurve,lpCurve*(-1)]),
				gate: t_gate
			);
			carRamp = EnvGen.kr(
				envelope: Env([0,1000, 0.000001], [0,rampDec], curve: \exp),
				gate: t_gate
			);
			carEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.01,carAtk,carRel], curve: [carCurve,carCurve*(-1)]),
				gate: t_gate
			);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			feedMod = SinOsc.ar(modHz, mul:modAmp*100) * modEnv;
			feedAmp = LinLin.kr(feedAmp,0,1,0.0,10.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			feedAmp = feedAmp * modAmp;
			rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
			amDepth = LinLin.kr(amDepth,0.0,1.0,0.0,2.0);

			feedCar = SinOsc.ar(carHz + feedMod + (carRamp*rampDepth)) * carEnv * (feedAmp/modAmp * 127);
			mod_1 = SinOsc.ar(modHz + feedCar, mul:modAmp*100) * modEnv;
			// mod_2 = SinOsc.ar(modHzThird + feedCar, mul:modAmp*100) * modEnv;
			// mod_3 = SinOsc.ar(modHzSeventh + feedCar, mul:modAmp*100) * modEnv;

			car = SinOsc.ar(carHz + mod_1 + (carRamp*rampDepth)) * carEnv;
			// carThird = SinOsc.ar(carHzThird + mod_2 + (carRamp*rampDepth)) * carEnv;
			// carSeventh = SinOsc.ar(carHzSeventh + mod_3 + (carRamp*rampDepth)) * carEnv;

			// car = (car * 0.5) + (carThird * 0.32) + (carSeventh * 0.18);

			noiseEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.01,noiseAtk,noiseRel], curve: [noiseCurve,noiseCurve*(-1)]),
				gate: t_gate
			);
			noise = BPF.ar(WhiteNoise.ar(0.24),8000,1.3) * (noiseAmp*noiseEnv);
			noise = BPeakEQ.ar(in:noise,freq:eqHz,rq:1,db:eqAmp,mul:1);
			noise = RLPF.ar(in:noise, freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			noise = RHPF.ar(in:noise,freq:hpHz, rq: filterQ, mul:1);

			ampMod = SinOsc.ar(freq:amHz,mul:(amDepth/2),add:1);
			car = car * ampMod;
			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			noise = Squiz.ar(in:noise, pitchratio:squishPitch, zcperchunk:squishChunk*100, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);

			car = Compander.ar(in:car, control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSendCar = Pan2.ar(car,pan);
			mainSendCar = mainSendCar * amp * LinLin.kr(velocity,0,127,0.0,1.0);

			noise = Compander.ar(in:noise, control:noise, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSendNoise = Pan2.ar(noise,pan);
			mainSendNoise = mainSendNoise * amp * LinLin.kr(velocity,0,127,0.0,1.0);

			delEnv = Select.kr(
				delayEnv > 0,[
					delaySend,
					(delaySend * EnvGen.kr(
						envelope: Env.new([0,0,1,0], times: [0.01,delayAtk,delayRel]),
						gate: t_gate)
					)
				]
			);

			Out.ar(out, mainSendCar);
			Out.ar(delayAuxL, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(delayAuxR, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(feedbackAux, (mainSendCar * feedbackSend));

			Out.ar(out, mainSendNoise);
			Out.ar(delayAuxL, (noise * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(delayAuxR, (noise * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(feedbackAux, (mainSendNoise * feedbackSend));

		}).send;
	}
}