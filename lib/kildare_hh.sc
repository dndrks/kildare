KildareHH {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	init {
		SynthDef(\kildare_hh, {
			arg out = 0, stopGate = 1,
			delayAuxL, delayAuxR, delaySend,
			delayAtk, delayRel,
			feedbackAux,feedbackSend,
			velocity, amp,
			carHz, thirdHz, seventhHz,
			carDetune, carAtk, carRel,
			tremDepth, tremHz,
			modAmp, modHz, modAtk, modRel,
			modFollow, modNum, modDenum,
			feedAmp,
			amDepth, amHz,
			eqHz, eqAmp,
			bitRate, bitCount,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpDepth,
			pan,
			squishPitch, squishChunk;

			var car, carThird, carSeventh,
			modHzThird, modHzSeventh,
			mod_1, mod_2, mod_3,
			carEnv, modEnv, carRamp, tremolo, tremod,
			ampMod, filterEnv, delayEnv, mainSend;

			amp = amp*0.85;
			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			delaySend = delaySend.lag3(0.1);
			feedbackSend = feedbackSend.lag3(0.1);

			filterQ = LinLin.kr(filterQ,0,100,1.0,0.001);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			tremDepth = LinLin.kr(tremDepth,0.0,100,0.0,1.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

			carHz = carHz * (2.pow(carDetune/12));
			thirdHz = thirdHz * (2.pow(carDetune/12));
			seventhHz = seventhHz * (2.pow(carDetune/12));

			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);
			modHzThird = Select.kr(modFollow > 0, [modHz, thirdHz * (modNum / modDenum)]);
			modHzSeventh = Select.kr(modFollow > 0, [modHz, seventhHz * (modNum / modDenum)]);

			modEnv = EnvGen.kr(Env.perc(modAtk, modRel));
			carRamp = EnvGen.kr(Env([1000, 0.000001], [tremHz], curve: \exp));
			carEnv = EnvGen.kr(Env.perc(carAtk, carRel), gate: stopGate, doneAction:2);
			filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);
			ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);

			mod_1 = SinOsc.ar(modHz, mul:modAmp) * modEnv;
			mod_2 = SinOsc.ar(modHzThird, mul:modAmp) * modEnv;
			mod_3 = SinOsc.ar(modHzSeventh, mul:modAmp) * modEnv;

			car = SinOscFB.ar(carHz + mod_1, feedAmp) * carEnv * amp;
			carThird = SinOscFB.ar(thirdHz + mod_2, feedAmp) * carEnv * amp;
			carSeventh = SinOscFB.ar(seventhHz + mod_3, feedAmp) * carEnv * amp;

			car = (car * 0.5) + (carThird * 0.32) + (carSeventh * 0.18);

			car = HPF.ar(car,1100,1);
			car = car*ampMod;
			tremolo = SinOsc.ar(tremHz, 0, tremDepth);
			tremod = (1.0 - tremDepth) + tremolo;
			car = car*tremod;
			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);

			car = Compander.ar(in:car,control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			mainSend = Pan2.ar(car,pan);
			mainSend = mainSend * (amp * LinLin.kr(velocity,0,127,0.0,1.0));

			delayEnv = (delaySend * EnvGen.kr(Env.perc(delayAtk, delayRel, 1),gate: stopGate));

			Out.ar(out, mainSend);
			Out.ar(delayAuxL, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delayEnv));
			Out.ar(delayAuxR, (car * amp * LinLin.kr(velocity,0,127,0.0,1.0) * delayEnv));
			Out.ar(feedbackAux, (mainSend * feedbackSend));

		}).send;
	}
}