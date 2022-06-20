Kildare {
	classvar <voiceKeys;
	classvar <synthDefs;
	const <numVoices = 3;

	var <paramProtos;
	var <groups;
	var <topGroup;

	var <outputSynths;
	var <busses;
	var <delayBufs;
	var <delayParams;
	var <reverbParams;
	var <mainOutParams;

	var <voiceTracker;
	classvar <indexTracker;

	*initClass {
		voiceKeys = [ \bd, \sd, \tm, \cp, \rs, \cb, \hh ];
		StartUp.add {
			var s = Server.default;

			s.waitForBoot {
				synthDefs = Dictionary.new;

				synthDefs[\bd] = SynthDef.new(\kildare_bd, {
					arg out = 0, stopGate = 1,
					delayAuxL, delayAuxR, delaySend,
					delayAtk, delayRel,
					reverbAux, reverbSend,
					amp, carHz, carDetune, carAtk, carRel,
					modHz, modAmp, modAtk, modRel, feedAmp,
					modFollow, modNum, modDenum,
					pan, rampDepth, rampDec,
					squishPitch, squishChunk,
					amDepth, amHz,
					eqHz, eqAmp, bitRate, bitCount,
					lpHz, hpHz, filterQ,
					lpAtk, lpRel, lpDepth;

					var car, mod, carEnv, modEnv, carRamp,
					feedMod, feedCar, ampMod, click, clicksound,
					mod_1, filterEnv, delayEnv, mainSend;

					eqHz = eqHz.lag3(0.1);
					lpHz = lpHz.lag3(0.1);
					hpHz = hpHz.lag3(0.1);
					delaySend = delaySend.lag3(0.1);
					reverbSend = reverbSend.lag3(0.1);
					modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

					filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
					modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
					feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
					eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
					rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
					amDepth = LinLin.kr(amDepth,0.0,1.0,0.0,2.0);
					carHz = carHz * (2.pow(carDetune/12));

					modEnv = EnvGen.kr(Env.perc(modAtk, modRel),gate: stopGate);
					filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);
					carRamp = EnvGen.kr(Env([1000, 0.000001], [rampDec], curve: \exp));
					carEnv = EnvGen.kr(envelope: Env.perc(carAtk, carRel),gate: stopGate, doneAction:2);

					mod_1 = SinOscFB.ar(
						modHz+ ((carRamp*3)*rampDepth),
						feedAmp,
						modAmp*10
					)* modEnv;

					car = SinOsc.ar(carHz + (mod_1) + (carRamp*rampDepth)) * carEnv;

					ampMod = SinOsc.ar(freq:amHz,mul:(amDepth/2),add:1);
					click = amp/4;
					clicksound = LPF.ar(Impulse.ar(0.003),16000,click) * EnvGen.kr(envelope: Env.perc(carAtk, 0.2), gate: stopGate);
					car = (car + clicksound)* ampMod;

					car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
					car = Decimator.ar(car,bitRate,bitCount,1.0);
					car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
					car = RLPF.ar(in:car,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
					car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);
					car = Compander.ar(in:car, control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

					mainSend = Pan2.ar(car,pan);
					mainSend = mainSend * amp;

					delayEnv = (delaySend * EnvGen.kr(Env.perc(delayAtk, delayRel, 1), gate: stopGate));

					Out.ar(out, mainSend);
					Out.ar(delayAuxL, (car * amp * delayEnv));
					Out.ar(delayAuxR, (car * amp * delayEnv));
					Out.ar(reverbAux, (mainSend * reverbSend));
				}).send;

				synthDefs[\sd] = SynthDef.new(\kildare_sd, {
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

					amp = amp/2;
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
					noise = BPF.ar(WhiteNoise.ar,8000,1.3) * (noiseAmp*noiseEnv);
					noise = BPeakEQ.ar(in:noise,freq:eqHz,rq:1,db:eqAmp,mul:1);
					noise = RLPF.ar(in:noise,freq:lpHz, rq: filterQ, mul:1);
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

				synthDefs[\tm] = SynthDef.new(\kildare_tm, {
					arg out = 0, stopGate = 1,
					delayAuxL, delayAuxR, delaySend,
					delayAtk, delayRel,
					reverbAux,reverbSend,
					carHz, carDetune, modHz, modAmp, modAtk, modRel, feedAmp,
					modFollow, modNum, modDenum,
					carAtk, carRel, amp,
					click,
					squishPitch, squishChunk,
					pan, rampDepth, rampDec, amDepth, amHz,
					eqHz, eqAmp, bitRate, bitCount,
					lpHz, hpHz, filterQ,
					lpAtk, lpRel, lpDepth;

					var car, mod, carEnv, modEnv, carRamp, feedMod,
					feedCar, ampMod, clicksound,
					mod_1, filterEnv, delayEnv, mainSend;

					amp = amp*0.5;
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
					mainSend = mainSend * amp;

					delayEnv = (delaySend * EnvGen.kr(Env.perc(delayAtk, delayRel, 1),gate: stopGate));

					Out.ar(out, mainSend);
					Out.ar(delayAuxL, (car * amp * delayEnv));
					Out.ar(delayAuxR, (car * amp * delayEnv));
					Out.ar(reverbAux, (mainSend * reverbSend));

				}).send;

				synthDefs[\cp] = SynthDef.new(\kildare_cp, {
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

				synthDefs[\rs] = SynthDef.new(\kildare_rs, {
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

					amp = amp*0.35;
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

				synthDefs[\cb] = SynthDef.new(\kildare_cb, {
					arg out = 0, stopGate = 1,
					delayAuxL, delayAuxR, delaySend,
					delayAtk, delayRel,
					reverbAux,reverbSend,
					amp, carHz, carDetune,
					modHz, modAmp, modAtk, modRel, feedAmp,
					modFollow, modNum, modDenum,
					carAtk, carRel,
					snap,
					pan, rampDepth, rampDec, amDepth, amHz,
					eqHz, eqAmp, bitRate, bitCount,
					lpHz, hpHz, filterQ,
					lpAtk, lpRel, lpDepth,
					squishPitch, squishChunk;

					var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar, ampMod,
					voice_1, voice_2, filterEnv, delayEnv, mainSend;

					amp = amp*0.6;
					eqHz = eqHz.lag3(0.1);
					lpHz = lpHz.lag3(0.1);
					hpHz = hpHz.lag3(0.1);
					delaySend = delaySend.lag3(0.1);
					reverbSend = reverbSend.lag3(0.1);

					carHz = carHz * (2.pow(carDetune/12));
					modHz = Select.kr(modFollow > 0, [modHz, carHz * (modNum / modDenum)]);

					filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
					feedAmp = LinLin.kr(feedAmp,0.0,1.0,1.0,3.0);
					eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
					rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
					amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);
					snap = LinLin.kr(snap,0.0,1.0,0.0,10.0);

					modEnv = EnvGen.kr(Env.perc(modAtk, modRel), gate:stopGate);
					carRamp = EnvGen.kr(Env([600, 0.000001], [rampDec], curve: \lin));
					carEnv = EnvGen.kr(Env.perc(carAtk, carRel),gate: stopGate);
					filterEnv = EnvGen.kr(Env.perc(lpAtk, lpRel, 1),gate: stopGate);

					voice_1 = LFPulse.ar((carHz) + (carRamp*rampDepth)) * carEnv * amp;
					voice_2 = SinOscFB.ar((carHz*1.5085)+ (carRamp*rampDepth),feedAmp) * carEnv * amp;
					ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);
					voice_1 = (voice_1+(LPF.ar(Impulse.ar(0.003),16000,1)*snap)) * ampMod;
					voice_1 = (voice_1*0.33)+(voice_2*0.33);
					voice_1 = Squiz.ar(in:voice_1, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
					voice_1 = Decimator.ar(voice_1,bitRate,bitCount,1.0);
					voice_1 = BPeakEQ.ar(in:voice_1,freq:eqHz,rq:1,db:eqAmp,mul:1);
					voice_1 = RLPF.ar(in:voice_1,freq:Clip.kr(lpHz + ((5*(lpHz * filterEnv)) * lpDepth), 20, 20000), rq: filterQ, mul:1);
					voice_1 = RHPF.ar(in:voice_1,freq:hpHz, rq: filterQ, mul:1);

					voice_1 = Compander.ar(in:voice_1,control:voice_1, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
					mainSend = Pan2.ar(voice_1,pan);
					mainSend = mainSend * amp;

					delayEnv = (delaySend * EnvGen.kr(Env.perc(delayAtk, delayRel, 1),gate: stopGate));

					Out.ar(out, mainSend);
					Out.ar(delayAuxL, (voice_1 * amp * delayEnv));
					Out.ar(delayAuxR, (voice_1 * amp * delayEnv));
					Out.ar(reverbAux, (mainSend * reverbSend));

					FreeSelf.kr(Done.kr(carEnv) * Done.kr(modEnv));

				}).send;

				synthDefs[\hh] = SynthDef(\kildare_hh, {
					arg out, stopGate = 1,
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

				}).add;

			} // Server.waitForBoot
		} // StartUp
	} // *initClass

	*new {
		^super.new.init;
	}

	init {
		var s = Server.default;

		outputSynths = Dictionary.new;

		voiceTracker = Dictionary.new;
		indexTracker = Dictionary.new;

		topGroup = Group.new(s);
		groups = Dictionary.new;
		voiceKeys.do({ arg voiceKey;
			groups[voiceKey] = Group.new(topGroup);
			indexTracker[voiceKey] = numVoices;
			numVoices.do{ arg i;
				voiceTracker[voiceKey] = Dictionary.new(numVoices);
			};
		});

		delayBufs = Dictionary.new;
		delayBufs[\initHit] = Buffer.alloc(s, s.sampleRate * 8.0, 2);
		delayBufs[\left1] = Buffer.alloc(s, s.sampleRate * 24.0, 2);
		delayBufs[\left2] = Buffer.alloc(s, s.sampleRate * 24.0, 2);
		delayBufs[\right] = Buffer.alloc(s, s.sampleRate * 24.0, 2);

		busses = Dictionary.new;
		busses[\mainOut] = Bus.audio(s, 2);
        busses[\reverbSend] = Bus.audio(s, 2);

		s.sync;

		busses[\delayLSend] = Bus.audio(s, 1);
		busses[\delayRSend] = Bus.audio(s, 1);

		delayParams = Dictionary.newFrom([
			\time, 0.8,
			\level, 1,
			\feedback, 0.7,
			\spread, 1,
			\lpHz, 20000,
			\hpHz, 20,
			\filterQ, 50,
			\reverbSend, 0
		]);

		reverbParams = Dictionary.newFrom([
			\preDelay, 0,
			\level, 1,
			\decay, 2,
			\earlyDiff, 0,
			\diffOffset, 0,
			\diffDiv, 10,
			\modFreq, 0.1,
			\modDepth, 0,
			\highCut, 8000,
			\lowCut, 10,
			\thresh, 0,
			\slopeBelow, 1,
			\slopeAbove, 1
		]);

		mainOutParams = Dictionary.newFrom([
			\lSHz, 20,
			\lSdb, 0,
			\lSQ, 1,
			\hSHz, 19000,
			\hSdb, 0,
			\hSQ, 1,
			\eqHz, 5000,
			\eqdb, 0,
			\eqQ, 1,
			\level, 1
		]);

		paramProtos = Dictionary.newFrom([
			\bd, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\reverbAux,busses[\reverbSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\reverbSend,0,
				\poly,0,
				\amp,0.7,
				\carHz,55,
				\carDetune,0,
				\carAtk,0,
				\carRel,0.3,
				\modAmp,0,
				\modHz,600,
				\modFollow,0,
				\modNum,1,
				\modDenum,1,
				\modAtk,0,
				\modRel,0.05,
				\feedAmp,1,
				\rampDepth,0.11,
				\rampDec,0.3,
				\squishPitch,1,
				\squishChunk,1,
				\amDepth,0,
				\amHz,8175.08,
				\eqHz,6000,
				\eqAmp,0,
				\bitRate,24000,
				\bitCount,24,
				\lpHz,19000,
				\hpHz,0,
				\filterQ,50,
				\lpAtk,0,
				\lpRel,0.3,
				\lpDepth,1,
				\pan,0,
			]),
			\sd, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\reverbAux,busses[\reverbSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\reverbSend,0,
				\poly,0,
				\amp,0.7,
				\carHz,282.54,
				\carDetune,0,
				\carAtk,0,
				\carRel,0.15,
				\modAmp,0,
				\modHz,2770,
				\modFollow,0,
				\modNum,1,
				\modDenum,1,
				\modAtk,0.2,
				\modRel,1,
				\feedAmp,0,
				\noiseAmp,0.01,
				\noiseAtk,0,
				\noiseRel,0.1,
				\rampDepth,0.5,
				\rampDec,0.06,
				\squishPitch,1,
				\squishChunk,1,
				\amDepth,0,
				\amHz,2698.8,
				\eqHz,12000,
				\eqAmp,1,
				\bitRate,24000,
				\bitCount,24,
				\lpHz,24000,
				\hpHz,0,
				\filterQ,50,
				\pan,0,
			]),
			\tm, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\reverbAux,busses[\reverbSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\reverbSend,0,
				\poly,0,
				\amp,0.7,
				\carHz,87.3,
				\carDetune,0,
				\carAtk,0,
				\carRel,0.43,
				\modAmp,0.32,
				\modHz,180,
				\modFollow,0,
				\modNum,1,
				\modDenum,1,
				\modAtk,0,
				\modRel,0.2,
				\feedAmp,1,
				\rampDepth,0.3,
				\rampDec,0.06,
				\squishPitch,1,
				\squishChunk,1,
				\amDepth,0,
				\amHz,2698.8,
				\eqHz,6000,
				\eqAmp,0,
				\bitRate,24000,
				\bitCount,24,
				\click,1,
				\lpHz,24000,
				\hpHz,20,
				\filterQ,50,
				\pan,0,
			]),
			\cp, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\reverbAux,busses[\reverbSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\reverbSend,0,
				\poly,0,
				\amp,0.7,
				\carHz,1600,
				\carDetune,0,
				\carRel,0.43,
				\modAmp,1,
				\modHz,300,
				\modFollow,0,
				\modNum,1,
				\modDenum,1,
				\modRel,0.5,
				\feedAmp,1,
				\click,0,
				\squishPitch,1,
				\squishChunk,1,
				\amDepth,0,
				\amHz,2698.8,
				\eqHz,6000,
				\eqAmp,0,
				\bitRate,24000,
				\bitCount,24,
				\click,1,
				\lpHz,24000,
				\hpHz,20,
				\filterQ,50,
				\pan,0,
			]),
			\rs, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\reverbAux,busses[\reverbSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\reverbSend,0,
				\poly,0,
				\amp,0.7,
				\carHz,370,
				\carDetune,0,
				\carAtk,0,
				\carRel,0.05,
				\modAmp,1,
				\modHz,4000,
				\modFollow,0,
				\modNum,1,
				\modDenum,1,
				\sdAmp,1,
				\sdAtk,0,
				\sdRel,0.05,
				\rampDepth,0,
				\rampDec,0.06,
				\squishPitch,1,
				\squishChunk,1,
				\amDepth,0,
				\amHz,8175.08,
				\eqHz,6000,
				\eqAmp,0,
				\bitRate,24000,
				\bitCount,24,
				\lpHz,19000,
				\hpHz,20,
				\filterQ,50,
				\pan,0,
			]),
			\cb, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\reverbAux,busses[\reverbSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\reverbSend,0,
				\poly,0,
				\amp,0.7,
				\carHz,404,
				\carDetune,0,
				\carAtk,0,
				\carRel,0.15,
				\feedAmp,0,
				\snap,0,
				\rampDepth,0,
				\rampDec,4,
				\squishPitch,1,
				\squishChunk,1,
				\amDepth,0,
				\amHz,2698.8,
				\eqHz,12000,
				\eqAmp,0,
				\bitRate,24000,
				\bitCount,24,
				\lpHz,24000,
				\hpHz,20,
				\filterQ,50,
				\pan,0,
			]),
			\hh, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\reverbAux,busses[\reverbSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\reverbSend,0,
				\poly,0,
				\amp,0.7,
				\carHz,200,
				\carDetune,0,
				\carAtk,0,
				\carRel,0.03,
				\modAmp,1,
				\modHz,100,
				\modFollow,0,
				\modNum,1,
				\modDenum,1,
				\modAtk,0,
				\modRel,2,
				\feedAmp,1,
				\tremDepth,1,
				\tremHz,1000,
				\squishPitch,1,
				\squishChunk,1,
				\amDepth,0,
				\amHz,8175.08,
				\eqHz,6000,
				\eqAmp,0,
				\bitRate,24000,
				\bitCount,24,
				\lpHz,19000,
				\hpHz,20,
				\filterQ,50,
				\pan,0,
			]),
		]);

		outputSynths[\delay] = SynthDef.new(\delay, {

			arg time = 0.3, level = 0.5, feedback = 0.8,
			lpHz = 19000, hpHz = 20, filterQ = 50,
			spread = 1, pan = 1,
			reverbSend = 0,
			inputL, inputR,
			mainOutput, reverbOutput;

			var delayL, delayR,
			leftBal, rightBal,
			leftInput, rightInput, startHit,
			leftPos, rightPos,
			feedbackDecayTime;

			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			level = level.lag3(0.1);

			leftInput = In.ar(inputL, 1);
			rightInput = In.ar(inputR, 1);

			filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
			leftPos = LinLin.kr(spread,0,1,0,-1);
			rightPos = LinLin.kr(spread,0,1,0,1);

			feedbackDecayTime = (0.001.log * time) / feedback.log;

			startHit = BufCombC.ar(delayBufs[\initHit].bufnum,leftInput,time/2,0,level);
			delayL = BufCombC.ar(delayBufs[\left1].bufnum,leftInput,time/2,0,level);
			delayL = BufCombC.ar(delayBufs[\left2].bufnum,delayL,time,feedbackDecayTime,level);
			delayR = BufCombC.ar(delayBufs[\right].bufnum,rightInput,time,feedbackDecayTime,level);

			startHit = RLPF.ar(in:startHit, freq:lpHz, rq: filterQ, mul:1);
			delayL = RLPF.ar(in:delayL, freq:lpHz, rq: filterQ, mul:1);
			delayR = RLPF.ar(in:delayR, freq:lpHz, rq: filterQ, mul:1);
			startHit = RHPF.ar(in:startHit, freq:hpHz, rq: filterQ, mul:1);
			delayL = RHPF.ar(in:delayL, freq:hpHz, rq: filterQ, mul:1);
			delayR = RHPF.ar(in:delayR, freq:hpHz, rq: filterQ, mul:1);

			leftPos = LinLin.kr(leftPos,0,-1,pan,-1);
			rightPos = LinLin.kr(rightPos,0,1,pan,1);
			leftBal = Pan2.ar(delayL+startHit,leftPos,0.5);
			rightBal = Pan2.ar(delayR,rightPos,0.5);

			leftBal = Compander.ar(in:leftBal,control:leftBal, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			rightBal = Compander.ar(in:rightBal,control:rightBal, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

			leftBal = leftBal + rightBal;
			Out.ar(mainOutput,leftBal);
			Out.ar(reverbOutput,leftBal * reverbSend);

        }).play(target:s, addAction:\addToTail, args:[
			\inputL, busses[\delayLSend],
			\inputR, busses[\delayRSend],
			\reverbOutput, busses[\reverbSend],
			\mainOutput, busses[\mainOut]
        ]);

        outputSynths[\reverb] = SynthDef.new(\reverb, {

			arg preDelay = 0.048, level = 0.5, decay = 6,
			earlyDiff = 0.707, diffDiv = 10, diffOffset = 0, modDepth = 0.2, modFreq = 0.1,
			highCut = 8000, lowCut = 20,
			thresh = 0, slopeBelow = 1, slopeAbove = 1,
			clampTime = 0.01, relaxTime = 0.1,
			in, out;

			var final, gated, sig, z, y,
			delayTimeBase, delayTimeRandVar, delayTimeSeeds,
			localin, localout, local, earlyReflections;

			highCut = highCut.lag3(0.1);
			lowCut = lowCut.lag3(0.1);

			// adapted from https://github.com/LMMS/lmms/blob/master/plugins/ReverbSC/revsc.c
			// Original Author(s): Sean Costello, Istvan Varga
			// Year: 1999, 2005
			// Location: Opcodes/reverbsc.c
			delayTimeBase = Dictionary.newFrom([
				1, (2473.0 / 48000.0),
				2, (2767.0 / 48000.0),
				3, (3217.0 / 48000.0),
				4, (3557.0 / 48000.0),
				5, (3907.0 / 48000.0),
				6, (4127.0 / 48000.0),
				7, (2143.0 / 48000.0),
				8, (1933.0 / 48000.0)
			]);
			delayTimeRandVar = Dictionary.newFrom([
				1, 0.0010,
				2, 0.0011,
				3, 0.0017,
				4, 0.0006,
				5, 0.0010,
				6, 0.0011,
				7, 0.0017,
				8, 0.0006
			]);
			delayTimeSeeds = Dictionary.newFrom([
				1, 1966.0,
				2, 29491.0,
				3, 22937.0,
				4, 9830.0,
				5, 20643.0,
				6, 22937.0,
				7, 29491.0,
				8, 14417.0
			]);

			sig = In.ar(in,2);
			z = DelayL.ar(sig, 0.5, preDelay);

			8.do({
				arg pass;
				var lbase = delayTimeBase[pass+1] + ((delayTimeRandVar[pass+1] * delayTimeSeeds[pass+1])/32768.0),
				rbase = delayTimeBase[pass+1] + ((delayTimeRandVar[pass+1] * delayTimeSeeds[pass+1])/32768.0) - (0.0001);
				local = LocalIn.ar(2) + z;
				earlyReflections = 0;
				5.do({
					arg iter;
					var voice = iter.asInteger + 1,
					decTime = lbase/2 + LinLin.kr(SinOsc.kr(freq: modFreq, mul: modDepth),-1,1,0,lbase/2);
					earlyReflections = earlyReflections + CombL.ar(
						in: local,
						maxdelaytime: 0.1,
						delaytime: (decTime - (decTime*(voice/diffDiv))) + (diffOffset/100),
						decaytime: decay,
						mul: (1/5) * level * earlyDiff
					);
				});
				local = local + earlyReflections;
				y = RLPF.ar(
					AllpassL.ar(
						in: local,
						maxdelaytime: 0.5,
						delaytime: [
							lbase/2 + LinLin.kr(SinOsc.kr(freq: modFreq, mul: modDepth),-1,1,0,lbase/2),
							rbase/2 + LinLin.kr(SinOsc.kr(freq: modFreq, mul: modDepth),-1,1,0,rbase/2),
						],
						decaytime: decay
					),
					highCut);
				LocalOut.ar([local,local]);
				[local,local]
			});
			final = AllpassN.ar(y, 0.05, [0.05.rand, 0.05.rand], decay);
			final = RHPF.ar(final,lowCut);

			gated = Compander.ar(final,final,thresh,slopeBelow,slopeAbove,clampTime,relaxTime);
			Out.ar(out, gated * level);

        }).play(target:s, addAction:\addToTail, args:[
            \in, busses[\reverbSend], \out, busses[\mainOut]
        ]);

        outputSynths[\main] = SynthDef.new(\main, {
            arg in, out,
			lSHz, lSdb, lSQ,
			hSHz, hSdb, hSQ,
			eqHz, eqdb, eqQ,
			level = 1.0;
			var src = In.ar(in, 2);

			lSHz = lSHz.lag3(0.1);
			hSHz = hSHz.lag3(0.1);
			eqHz = eqHz.lag3(0.1);
			lSdb = lSdb.lag3(0.1);
			hSdb = hSdb.lag3(0.1);
			eqdb = eqdb.lag3(0.1);
			level = level.lag3(0.1);

			lSQ = LinLin.kr(lSQ,0,100,1.0,0.3);
			hSQ = LinLin.kr(hSQ,0,100,1.0,0.3);
			eqQ = LinLin.kr(eqQ,0,100,1.0,0.1);

			src = BLowShelf.ar(src, lSHz, lSQ, lSdb);
			src = BHiShelf.ar(src, hSHz, hSQ, hSdb);
			src = BPeakEQ.ar(src, eqHz, eqQ, eqdb);
			src = Limiter.ar(src,0.5);

			Out.ar(out, src * level);
        }).play(target:s, addAction:\addToTail, args: [
            \in, busses[\mainOut], \out, 0
        ]);


	}

	trigger { arg voiceKey;
		if( paramProtos[voiceKey][\poly] == 0,{
			groups[voiceKey].set(\stopGate, -1.05);
			indexTracker[voiceKey] = numVoices;
			Synth.new(\kildare_++voiceKey, paramProtos[voiceKey].getPairs, groups[voiceKey]);
		},{
			indexTracker[voiceKey] = (indexTracker[voiceKey] + 1)%numVoices;
			if (voiceTracker[voiceKey][indexTracker[voiceKey]].isNil.not, {
				if (voiceTracker[voiceKey][indexTracker[voiceKey]].isPlaying, {
					voiceTracker[voiceKey][indexTracker[voiceKey]].set(\stopGate, -1.05);
					// ("stopping previous iteration of  "++indexTracker[voiceKey]++voiceKey).postln;
				});
			});
			voiceTracker[voiceKey][indexTracker[voiceKey]] = Synth.new(\kildare_++voiceKey, paramProtos[voiceKey].getPairs, groups[voiceKey]);
			if (voiceTracker[voiceKey][indexTracker[voiceKey]].isNil.not, {
				NodeWatcher.register(voiceTracker[voiceKey][indexTracker[voiceKey]],true);
			});
		}
		);
	}

	setVoiceParam { arg voiceKey, paramKey, paramValue;
		if( paramProtos[voiceKey][\poly] == 0,{
			groups[voiceKey].set(paramKey, paramValue);
		});
		paramProtos[voiceKey][paramKey] = paramValue;
	}

	setDelayParam { arg paramKey, paramValue;
		delayParams[paramKey] = paramValue;
		outputSynths[\delay].set(paramKey, paramValue);
	}

	setReverbParam { arg paramKey, paramValue;
		reverbParams[paramKey] = paramValue;
		outputSynths[\reverb].set(paramKey, paramValue);
	}

	setMainParam { arg paramKey, paramValue;
		mainOutParams[paramKey] = paramValue;
		outputSynths[\main].set(paramKey, paramValue);
	}

	allNotesOff {
		topGroup.set(\stopGate, 0);
	}

	free {
		topGroup.free;
		synthDefs.do({arg def;
			def.free;
		});
		voiceTracker.do({arg voice;
			voice.free;
		});
		busses.do({arg bus;
			bus.free;
		});
		outputSynths.do({arg bus;
			bus.free;
		});
		delayBufs.do({arg buf;
			buf.free;
		});
	}

}