KildareCP {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	init {
		SynthDef(\kildare_cp, {
			arg out = 0, stopGate = 1,
			delayAuxL, delayAuxR, delaySend,
			delayAtk, delayRel,
			reverbAux,reverbSend,
			carHz, carDetune,
			modHz, modAmp, modRel, feedAmp,
			modFollow, modNum, modDenum,
			carRel, amp, click,
			squishPitch, squishChunk,
			pan, amDepth, amHz,
			eqHz, eqAmp, bitRate, bitCount,
			lpHz, hpHz, filterQ,
			lpAtk, lpRel, lpDepth;

			var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar, ampMod,
			mod_1, mod_2, filterEnv, delayEnv, mainSend;

			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			delaySend = delaySend.lag3(0.1);
			reverbSend = reverbSend.lag3(0.1);

			carHz = carHz * (2.pow(carDetune/12));
			modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

			filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
			modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
			feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

			modEnv = EnvGen.ar(
				Env.new(
					[0, 1, 0, 0.9, 0, 0.7, 0, 0.5, 0],
					[0.001, 0.009, 0, 0.008, 0, 0.01, 0, modRel],
					curve: \lin
				),gate: stopGate
			);
			filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);
			carRamp = EnvGen.kr(Env([600, 0.000001], [0], curve: \lin));
			carEnv = EnvGen.ar(
				Env.new(
					[0, 1, 0, 0.9, 0, 0.7, 0, 0.5, 0],
					[0,0,0,0,0,0,0,carRel/4],
					[0, -3, 0, -3, 0, -3, 0, -3]
				),gate: stopGate
			);

			mod_2 = SinOscFB.ar(
				(modHz*4),
				feedAmp,
				0,
				modAmp*1
			)* modEnv;

			mod_1 = SinOscFB.ar(
				modHz+mod_2,
				feedAmp,
				modAmp*100
			)* modEnv;

			car = SinOsc.ar(carHz + (mod_1)) * carEnv * amp;
			car = RHPF.ar(in:car+(LPF.ar(Impulse.ar(0.003),12000,1)*click),freq:hpHz,rq:filterQ,mul:1);

			ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);
			car = car * ampMod;
			car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			car = Decimator.ar(car,bitRate,bitCount,1.0);
			car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
			car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
			car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);

			car = car.softclip;
			mainSend = Pan2.ar(car,pan);
			mainSend = mainSend * amp;

			delayEnv = (delaySend * EnvGen.kr(Env.perc(delayAtk, delayRel, 1),gate: stopGate));

			Out.ar(out, mainSend);
			Out.ar(delayAuxL, (car * amp * delayEnv));
			Out.ar(delayAuxR, (car * amp * delayEnv));
			Out.ar(reverbAux, (mainSend * reverbSend));

			FreeSelf.kr(Done.kr(modEnv) * Done.kr(carEnv));

		}).send;
	}
}