KildareTM {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	init {
		SynthDef(\kildare_tm, {
			arg out = 0, stopGate = 1,
			delayAuxL, delayAuxR, delaySend,
			delayAtk, delayRel,
			reverbAux,reverbSend,
			velocity,
			carHz, carDetune, modHz, modAmp, modAtk, modRel, feedAmp,
			modFollow, modNum, modDenum,
			carAtk, carRel, amp,
			click = 1,
			squishPitch, squishChunk,
			pan, rampDepth, rampDec, amDepth, amHz,
			eqHz, eqAmp, bitRate, bitCount,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpDepth;

			var car, mod, carEnv, modEnv, carRamp, feedMod,
			feedCar, ampMod, clicksound,
			mod_1, filterEnv, delayEnv, mainSend;

			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			delaySend = delaySend.lag3(0.1);
			reverbSend = reverbSend.lag3(0.1);

			carHz = carHz * (2.pow(carDetune/12));
			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

			filterQ = LinLin.kr(filterQ,0,100,1.0,0.001);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

			modEnv = EnvGen.kr(Env.perc(modAtk, modRel));
			filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);
			carRamp = EnvGen.kr(Env([600, 0.000001], [rampDec], curve: \lin));
			carEnv = EnvGen.kr(Env.perc(carAtk, carRel), gate: stopGate, doneAction:2);

			mod_1 = SinOscFB.ar(
				modHz,
				feedAmp,
				modAmp*10
			)* modEnv;

			car = SinOsc.ar(carHz + (mod_1) + (carRamp*rampDepth)) * carEnv * amp;

			ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);
			clicksound = LPF.ar(Impulse.ar(0.003),16000,click) * EnvGen.kr(envelope: Env.perc(carAtk, 0.2),gate: stopGate);
			car = (car + clicksound) * ampMod;
			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);

			car = Compander.ar(in:car, control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSend = Pan2.ar(car,pan);
			mainSend = mainSend * amp * LinLin.kr(velocity,0,127,0.0,1.0);

			delayEnv = (delaySend * EnvGen.kr(Env.perc(delayAtk, delayRel, 1),gate: stopGate));

			Out.ar(out, mainSend);
			Out.ar(delayAuxL, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delayEnv));
			Out.ar(delayAuxR, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delayEnv));
			Out.ar(reverbAux, (mainSend * reverbSend));
		}).send;
	}
}