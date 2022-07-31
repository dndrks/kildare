KildareSD {

	*initClass {
		StartUp.add {
			var s = Server.default;
			s.waitForBoot {
				SynthDef(\kildare_sd, {
					arg out = 0, stopGate = 1,
					delayAuxL, delayAuxR, delaySend,
					delayAtk, delayRel,
					reverbAux,reverbSend,
					carHz, carDetune, carAtk, carRel,
					modHz, modAmp, modAtk, modRel, feedAmp,
					modFollow, modNum, modDenum,
					amp, pan,
					rampDepth, rampDec, noiseAmp,
					noiseAtk, noiseRel, bitRate, bitCount,
					eqHz,eqAmp,
					squishPitch, squishChunk,
					lpHz, hpHz, filterQ,
					lpAtk, lpRel, lpDepth,
					amDepth, amHz;

					var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar,
					noise, noiseEnv, mix, ampMod, filterEnv, delayEnv, mainSendCar, mainSendNoise;

					amp = amp;
					noiseAmp = noiseAmp/2;
					eqHz = eqHz.lag3(0.1);
					lpHz = lpHz.lag3(0.1);
					hpHz = hpHz.lag3(0.1);
					delaySend = delaySend.lag3(0.1);
					reverbSend = reverbSend.lag3(0.1);

					carHz = carHz * (2.pow(carDetune/12));
					modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

					filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
					modEnv = EnvGen.kr(Env.perc(modAtk, modRel));
					filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);
					carRamp = EnvGen.kr(Env([1000, 0.000001], [rampDec], curve: \exp));
					carEnv = EnvGen.kr(Env.perc(carAtk, carRel),gate: stopGate);
					modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
					feedMod = SinOsc.ar(modHz, mul:modAmp*100) * modEnv;
					feedAmp = LinLin.kr(feedAmp,0,1,0.0,10.0);
					eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
					feedAmp = feedAmp * modAmp;
					rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
					amDepth = LinLin.kr(amDepth,0.0,1.0,0.0,2.0);

					feedCar = SinOsc.ar(carHz + feedMod + (carRamp*rampDepth)) * carEnv * (feedAmp/modAmp * 127);
					mod = SinOsc.ar(modHz + feedCar, mul:modAmp*100) * modEnv;
					car = SinOsc.ar(carHz + mod + (carRamp*rampDepth)) * carEnv;

					noiseEnv = EnvGen.kr(Env.perc(noiseAtk,noiseRel),gate: stopGate);
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
					mainSendCar = mainSendCar * amp;

					noise = Compander.ar(in:noise, control:noise, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
					mainSendNoise = Pan2.ar(noise,pan);
					mainSendNoise = mainSendNoise * amp;

					delayEnv = (delaySend * EnvGen.kr(Env.perc(delayAtk, delayRel, 1),gate: stopGate));

					Out.ar(out, mainSendCar);
					Out.ar(delayAuxL, (car * amp * delayEnv));
					Out.ar(delayAuxR, (car * amp * delayEnv));
					Out.ar(reverbAux, (mainSendCar * reverbSend));

					Out.ar(out, mainSendNoise);
					Out.ar(delayAuxL, (noise * amp * delayEnv));
					Out.ar(delayAuxR, (noise * amp * delayEnv));
					Out.ar(reverbAux, (mainSendNoise * reverbSend));

					FreeSelf.kr(Done.kr(carEnv) * Done.kr(noiseEnv));

				}).send;
			} // s.waitForBoot
		} // StartUp
	} // *initClass

}