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
			reverbAux,reverbSend,
			amp, carHz, carDetune, carAtk, carRel,
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

			var car, mod, carEnv, modEnv, carRamp, tremolo, tremod,
			ampMod, filterEnv, delayEnv, mainSend;

			amp = amp*0.85;
			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			delaySend = delaySend.lag3(0.1);
			reverbSend = reverbSend.lag3(0.1);

			filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			tremDepth = LinLin.kr(tremDepth,0.0,100,0.0,1.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);
			carHz = carHz * (2.pow(carDetune/12));
			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

			modEnv = EnvGen.kr(Env.perc(modAtk, modRel));
			carRamp = EnvGen.kr(Env([1000, 0.000001], [tremHz], curve: \exp));
			carEnv = EnvGen.kr(Env.perc(carAtk, carRel), gate: stopGate, doneAction:2);
			filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);
			ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);
			mod = SinOsc.ar(modHz, mul:modAmp) * modEnv;
			car = SinOscFB.ar(carHz + mod, feedAmp) * carEnv * amp;
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
			mainSend = mainSend * amp;

			delayEnv = (delaySend * EnvGen.kr(Env.perc(delayAtk, delayRel, 1),gate: stopGate));

			Out.ar(out, mainSend);
			Out.ar(delayAuxL, (car * amp * delayEnv));
			Out.ar(delayAuxR, (car * amp * delayEnv));
			Out.ar(reverbAux, (mainSend * reverbSend));

		}).send;
	}
}