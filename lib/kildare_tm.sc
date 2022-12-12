KildareTM {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	init {

		SynthDef(\kildare_tm, {
			arg out = 0, t_gate = 0,
			delayAuxL, delayAuxR, delaySend,
			delayEnv, delayAtk, delayRel,
			feedbackAux,feedbackSend,
			velocity = 127,
			carHz, carHzThird, carHzSeventh,
			carDetune, modHz, modAmp, modAtk, modRel, modCurve = -4, feedAmp,
			modFollow, modNum, modDenum,
			carAtk, carRel, carCurve = -4, amp,
			click = 1,
			squishPitch, squishChunk,
			pan, rampDepth, rampDec, amDepth, amHz,
			eqHz, eqAmp, bitRate, bitCount,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpCurve = -4, lpDepth;

			var car, carThird, carSeventh,
			mod, modHzThird, modHzSeventh,
			carEnv, modEnv, carRamp,
			feedMod, feedCar, ampMod, clicksound,
			mod_1, mod_2, mod_3,
			filterEnv, delEnv, mainSend;

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
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

			modEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.01,modAtk,modRel], curve: [modCurve,modCurve*(-1)]),
				gate: t_gate
			);
			filterEnv = EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.01,lpAtk,lpRel], curve: [lpCurve,lpCurve*(-1)]),
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

			mod_1 = SinOscFB.ar(
				modHz + ((carRamp*3)*rampDepth),
				feedAmp,
				modAmp*10
			)* modEnv;

			mod_2 = SinOscFB.ar(
				modHzThird + ((carRamp*3)*rampDepth),
				feedAmp,
				modAmp*10
			)* modEnv;

			mod_3 = SinOscFB.ar(
				modHzSeventh + ((carRamp*3)*rampDepth),
				feedAmp,
				modAmp*10
			)* modEnv;

			car = SinOsc.ar(carHz + (mod_1) + (carRamp*rampDepth)) * carEnv;
			carThird = SinOsc.ar(carHzThird + (mod_2) + (carRamp*rampDepth)) * carEnv;
			carSeventh = SinOsc.ar(carHzSeventh + (mod_3) + (carRamp*rampDepth)) * carEnv;

			car = (car * 0.5) + (carThird * 0.32) + (carSeventh * 0.18);

			ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);
			clicksound = LPF.ar(Impulse.ar(0.003),16000,click) * EnvGen.kr(
				envelope: Env.new([0,0,1,0], times: [0.0,carAtk,0.2], curve: [carCurve,carCurve*(-1)]),
				gate: t_gate
			);

			car = (car + clicksound) * ampMod;
			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);

			car = Compander.ar(in:car, control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSend = Pan2.ar(car,pan);
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
			Out.ar(delayAuxL, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(delayAuxR, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delEnv));
			Out.ar(feedbackAux, (mainSend * feedbackSend));
		}).send;
	}
}