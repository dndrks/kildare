KildareSaw {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	init {

		SynthDef(\kildare_saw, {
			arg out = 0, stopGate = 1,
			delayAuxL, delayAuxR, delaySend,
			delayAtk, delayRel,
			feedbackAux, feedbackSend,
			velocity, amp,
			carHz, thirdHz, seventhHz,
			subAmp = 1, subPw = 0.5,
			phaseOff1 = 2/3, phaseOff2 = 4/3,
			carDetune, carAtk, carRel, carCurve = -4,
			modHz, modAmp, modAtk, modRel, modCurve = -4, feedAmp,
			modFollow, modNum, modDenum,
			pan, rampDepth, rampDec,
			squishPitch, squishChunk,
			amDepth, amHz,
			eqHz, eqAmp, bitRate, bitCount,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpCurve = -4, lpDepth;

			var car, carThird, carSeventh, subOsc,
			mod, modHzThird, modHzSeventh,
			carEnv, modEnv, carRamp,
			feedMod, feedCar, ampMod, click, clicksound,
			mod_1, mod_2, mod_3,
			filterEnv, delayEnv, mainSend;

			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			delaySend = delaySend.lag3(0.1);
			feedbackSend = feedbackSend.lag3(0.1);
			modHz = (modHz * (1 - modFollow)) + (carHz * modFollow * modDenum);
			modHzThird = (modHz * (1 - modFollow)) + (thirdHz * modFollow * modDenum);
			modHzSeventh = (modHz * (1 - modFollow)) + (seventhHz * modFollow * modDenum);

			filterQ = LinLin.kr(filterQ,0,100,1.0,0.001);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127).lag3(0.1);
			feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
			amDepth = LinLin.kr(amDepth,0.0,1.0,0.0,2.0);
			carHz = (carHz * (1 - modFollow)) + (carHz * modFollow * modNum);
			thirdHz = (thirdHz * (1 - modFollow)) + (thirdHz * modFollow * modNum);
			seventhHz = (seventhHz * (1 - modFollow)) + (seventhHz * modFollow * modNum);
			carHz = carHz * (2.pow(carDetune/12));
			thirdHz = thirdHz * (2.pow(carDetune/12));
			seventhHz = seventhHz * (2.pow(carDetune/12));

			modEnv = EnvGen.kr(Env.perc(modAtk, modRel, curve: modCurve),gate: stopGate);
			filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, curve: lpCurve),gate: stopGate);
			carRamp = EnvGen.kr(Env([1000, 0.000001], [rampDec], curve: \exp));
			carEnv = EnvGen.kr(envelope: Env.perc(carAtk, carRel, curve: carCurve), gate: stopGate, doneAction:2);

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

			car = LFSaw.ar(carHz + (mod_1) + (carRamp*rampDepth),0) * carEnv;
			carThird = LFSaw.ar(thirdHz + (mod_2) + (carRamp*rampDepth), phaseOff1) * carEnv;
			carSeventh = LFSaw.ar(seventhHz + (mod_3) + (carRamp*rampDepth), phaseOff2) * carEnv;
			car = (car * 0.5) + (carThird * 0.32) + (carSeventh * 0.18);

			subOsc = Pulse.ar(freq: carHz/2, width: subPw, mul: subAmp * amp) * carEnv;
			car = car + subOsc;

			ampMod = SinOsc.ar(freq:amHz,mul:(amDepth/2),add:1);
			car = car* ampMod;

			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);
			car = Compander.ar(in:car, control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

			mainSend = Pan2.ar(car,pan);
			mainSend = mainSend * (amp * LinLin.kr(velocity,0,127,0.0,1.0));

			delayEnv = (delaySend * EnvGen.kr(Env.perc(delayAtk, delayRel, 1), gate: stopGate));

			Out.ar(out, LeakDC.ar(mainSend));
			Out.ar(delayAuxL, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delayEnv));
			Out.ar(delayAuxR, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delayEnv));
			Out.ar(feedbackAux, (mainSend * feedbackSend));
		}).send;
	}
}