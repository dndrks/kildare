
// does all heavy lifing - defines synthdefs, etc
// norns-agnostic
// doesn't need Crone, CroneEngine
// works on yr mac

Kildare {
	classvar <voiceKeys;
	classvar <synthDefs;

	// [EB] we want these to be specific to the instance:
	// - keep a dictionary of prototype paraemter values
	var <paramProtos;
	// - keep a group for each voice type
	var <groups;
	// - top-level group to easily free everything
	var <topGroup;

	var <sendKeys;

	*initClass {
		voiceKeys = [ \bd, \sd, \tm, \cp, \rs, \cb, \hh ];
		StartUp.add {
			var s = Server.default;

			// [EB] need to make sure the server is running b4 asking it to do stuff
			s.waitForBoot {
				synthDefs = Dictionary.new;

				// [EB] added prefix to synthdefs.
				// an even better alternative would be to make a random name,
				// and instantiate via the synthdef directly. but this is fine
				synthDefs[\bd] = SynthDef.new(\kildare_bd, {
					arg stopGate = 1,
					amp, carHz, carAtk, carRel,
					modHz, modAmp, modAtk, modRel, feedAmp,
					pan, rampDepth, rampDec,
					squishPitch, squishChunk,
					amDepth, amHz,
					eqHz, eqAmp, brate, bcnt,
					lpHz, hpHz, filterQ;

					var car, mod, carEnv, modEnv, carRamp,
					feedMod, feedCar, ampMod, click, clicksound,
					mod_1;

					lpHz = lpHz.lag3(1);
					hpHz = hpHz.lag3(1);
					pan = pan.lag2(0.1);

					filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
					modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
					feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
					eqAmp = LinLin.kr(eqAmp,0.0,1.0,0.0,10.0);
					rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
					amDepth = LinLin.kr(amDepth,0.0,1.0,0.0,2.0);

					modEnv = EnvGen.kr(Env.perc(modAtk, modRel),gate: stopGate);
					carRamp = EnvGen.kr(Env([1000, 0.000001], [rampDec], curve: \exp));
					carEnv = EnvGen.kr(envelope: Env.perc(carAtk, carRel),gate: stopGate, doneAction:2);

					mod_1 = SinOscFB.ar(
						modHz+ ((carRamp*3)*rampDepth),
						feedAmp,
						modAmp*10
					)* modEnv;

					car = SinOsc.ar(carHz + (mod_1) + (carRamp*rampDepth)) * carEnv * amp;

					ampMod = SinOsc.ar(freq:amHz,mul:(amDepth/2),add:1);
					click = amp/4;
					clicksound = LPF.ar(Impulse.ar(0.003),16000,click) * EnvGen.kr(envelope: Env.perc(carAtk, 0.2), gate: stopGate);
					car = (car + clicksound)* ampMod;

					car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
					car = Decimator.ar(car,brate,bcnt,1.0);
					car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
					car = BLowPass.ar(in:car,freq:lpHz, rq: filterQ, mul:1);
					car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);
					car = Compander.ar(in:car,control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
					car = Pan2.ar(car,pan);
					Out.ar(0, car);
				}).send;

				synthDefs[\sd] = SynthDef.new(\kildare_sd, {
					arg stopGate = 1,
					carHz, modHz, modAmp, modAtk,
					modRel, carAtk, carRel, amp, pan,
					rampDepth, rampDec, feedAmp, noiseAmp,
					noiseAtk, noiseRel, brate, bcnt,
					eqHz,eqAmp,
					squishPitch, squishChunk,
					lpHz, hpHz, filterQ,
					amDepth, amHz;

					var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar,
					noise, noiseEnv, mix, ampMod;

					amp = amp/2;
					noiseAmp = noiseAmp/2;
					lpHz = lpHz.lag3(1);
					hpHz = hpHz.lag3(1);
					pan = pan.lag2(0.1);

					filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
					modEnv = EnvGen.kr(Env.perc(modAtk, modRel));
					carRamp = EnvGen.kr(Env([1000, 0.000001], [rampDec], curve: \exp));
					carEnv = EnvGen.kr(Env.perc(carAtk, carRel),gate: stopGate);
					modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
					feedMod = SinOsc.ar(modHz, mul:modAmp*100) * modEnv;
					feedAmp = LinLin.kr(feedAmp,0,1,0.0,10.0);
					eqAmp = LinLin.kr(eqAmp,0.0,1.0,0.0,10.0);
					feedAmp = feedAmp * modAmp; // 220224
					rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
					amDepth = LinLin.kr(amDepth,0.0,1.0,0.0,2.0);

					feedCar = SinOsc.ar(carHz + feedMod + (carRamp*rampDepth)) * carEnv * (feedAmp/modAmp * 127);
					mod = SinOsc.ar(modHz + feedCar, mul:modAmp*100) * modEnv;
					car = SinOsc.ar(carHz + mod + (carRamp*rampDepth)) * carEnv * amp;

					noiseEnv = EnvGen.kr(Env.perc(noiseAtk,noiseRel),gate: stopGate);
					noise = BPF.ar(WhiteNoise.ar,8000,1.3) * (noiseAmp*noiseEnv);
					noise = BPeakEQ.ar(in:noise,freq:eqHz,rq:1,db:eqAmp,mul:1);
					noise = BLowPass.ar(in:noise,freq:lpHz, rq: filterQ, mul:1);
					noise = RHPF.ar(in:noise,freq:hpHz, rq: filterQ, mul:1);

					ampMod = SinOsc.ar(freq:amHz,mul:(amDepth/2),add:1);
					car = car * ampMod;
					car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
					noise = Squiz.ar(in:noise, pitchratio:squishPitch, zcperchunk:squishChunk*100, mul:1);
					car = Decimator.ar(car,brate,bcnt,1.0);
					car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
					car = BLowPass.ar(in:car,freq:lpHz, rq: filterQ, mul:1);
					car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);
					mix = car;
					mix = Compander.ar(in:mix,control:mix, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
					noise = Compander.ar(in:noise,control:noise, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

					Out.ar(0, Pan2.ar(mix,pan));
					Out.ar(0, Pan2.ar(noise,pan));
					FreeSelf.kr(Done.kr(carEnv) * Done.kr(noiseEnv));
				}).send;

				synthDefs[\tm] = SynthDef.new(\kildare_tm, {
					arg stopGate = 1,
					carHz, modHz, modAmp, modAtk, modRel, feedAmp,
					carAtk, carRel, amp,
					click,
					squishPitch, squishChunk,
					pan, rampDepth, rampDec, amDepth, amHz,
					eqHz, eqAmp, brate, bcnt,
					lpHz, hpHz, filterQ;

					var car, mod, carEnv, modEnv, carRamp, feedMod,
					feedCar, ampMod, clicksound,
					mod_1;

					amp = amp*0.66;
					lpHz = lpHz.lag3(1);
					hpHz = hpHz.lag3(1);
					pan = pan.lag2(0.1);

					filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
					modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
					feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
					eqAmp = LinLin.kr(eqAmp,0.0,1.0,0.0,10.0);
					rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
					amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

					modEnv = EnvGen.kr(Env.perc(modAtk, modRel));
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
					car = Decimator.ar(car,brate,bcnt,1.0);
					car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
					car = BLowPass.ar(in:car,freq:lpHz, rq: filterQ, mul:1);
					car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);
					car = Compander.ar(in:car,control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

					Out.ar(0, Pan2.ar(car,pan));
				}).send;

				synthDefs[\cp] = SynthDef.new(\kildare_cp, {
					arg stopGate = 1,
					carHz,
					modHz, modAmp, modRel, feedAmp,
					carRel, amp, click,
					squishPitch, squishChunk,
					pan, amDepth, amHz,
					eqHz, eqAmp, brate, bcnt,
					lpHz, hpHz, filterQ;

					var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar, ampMod,
					mod_1,mod_2;

					lpHz = lpHz.lag3(1);
					hpHz = hpHz.lag3(1);
					pan = pan.lag2(0.1);

					filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
					modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
					feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
					eqAmp = LinLin.kr(eqAmp,0.0,1.0,0.0,10.0);
					amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

					modEnv = EnvGen.ar(
						Env.new(
							[0, 1, 0, 0.9, 0, 0.7, 0, 0.5, 0],
							[0.001, 0.009, 0, 0.008, 0, 0.01, 0, modRel],
							curve: \lin
						),gate: stopGate
					);
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
					car = Decimator.ar(car,brate,bcnt,1.0);
					car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
					car = BLowPass.ar(in:car,freq:lpHz, rq: filterQ, mul:1);
					car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);
					car = car.softclip;

					Out.ar(0, Pan2.ar(car,pan));
					FreeSelf.kr(Done.kr(modEnv) * Done.kr(carEnv));
				}).send;

				synthDefs[\rs] = SynthDef.new(\kildare_rs, {
					arg stopGate = 1,
					carHz,
					modHz, modAmp,
					carAtk, carRel, amp,
					pan, rampDepth, rampDec, amDepth, amHz,
					eqHz, eqAmp, brate, bcnt,
					sdAmp, sdRel, sdAtk,
					lpHz, hpHz, filterQ,
					squishPitch, squishChunk;

					var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar, ampMod,
					mod_1,mod_2,feedAmp,feedAMP, sd_modHz,
					sd_car, sd_mod, sd_carEnv, sd_modEnv, sd_carRamp, sd_feedMod, sd_feedCar, sd_noise, sd_noiseEnv,
					sd_mix;

					amp = amp*0.35;
					lpHz = lpHz.lag3(1);
					hpHz = hpHz.lag3(1);
					pan = pan.lag2(0.1);

					filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
					modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
					eqAmp = LinLin.kr(eqAmp,0.0,1.0,0.0,10.0);
					rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
					amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

					feedAmp = modAmp.linlin(0, 127, 0, 3);
					feedAMP = modAmp.linlin(0, 127, 0, 4);

					carRamp = EnvGen.kr(Env([600, 0.000001], [rampDec], curve: \lin));
					carEnv = EnvGen.kr(Env.perc(carAtk, carRel),gate: stopGate);

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
					car = Decimator.ar(Pan2.ar(car,pan),brate,bcnt,1.0);
					car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
					car = BLowPass.ar(in:car,freq:lpHz, rq: filterQ, mul:1);
					car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);
					car = LPF.ar(car,12000,1);
					car = car.softclip;
					car = Compander.ar(in:car,control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

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
					sd_mix = Decimator.ar(sd_mix,brate,bcnt,1.0);
					sd_mix = BPeakEQ.ar(in:sd_mix,freq:eqHz,rq:1,db:eqAmp,mul:1);
					sd_mix = BLowPass.ar(in:sd_mix,freq:lpHz, rq: filterQ, mul:1);
					sd_mix = RHPF.ar(in:sd_mix,freq:hpHz, rq: filterQ, mul:1);
					sd_mix = sd_mix.softclip;
					sd_mix = Compander.ar(in:sd_mix,control:sd_mix, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

					Out.ar(0, Pan2.ar(car,pan));
					Out.ar(0, Pan2.ar(sd_mix,pan));

					FreeSelf.kr(Done.kr(sd_carEnv) * Done.kr(carEnv));
				}).send;

				synthDefs[\cb] = SynthDef.new(\kildare_cb, {
					arg stopGate = 1,
					amp, carHz,
					modHz, modAmp, modAtk, modRel, feedAmp,
					carAtk, carRel,
					snap,
					pan, rampDepth, rampDec, amDepth, amHz,
					eqHz, eqAmp, brate, bcnt,
					lpHz, hpHz, filterQ,
					squishPitch, squishChunk;

					var car, mod, carEnv, modEnv, carRamp, feedMod, feedCar, ampMod,
					sig,voice_1,voice_2,klank_env,other_mod1,other_mod2;

					amp = amp*0.6;
					lpHz = lpHz.lag3(1);
					hpHz = hpHz.lag3(1);
					pan = pan.lag2(0.1);

					filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
					feedAmp = LinLin.kr(feedAmp,0.0,1.0,1.0,3.0);
					eqAmp = LinLin.kr(eqAmp,0.0,1.0,0.0,10.0);
					rampDepth = LinLin.kr(rampDepth,0.0,1.0,0.0,2.0);
					amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);
					snap = LinLin.kr(snap,0.0,1.0,0.0,10.0);

					modEnv = EnvGen.kr(Env.perc(modAtk, modRel), gate:stopGate);
					carRamp = EnvGen.kr(Env([600, 0.000001], [rampDec], curve: \lin));
					carEnv = EnvGen.kr(Env.perc(carAtk, carRel),gate: stopGate);

					voice_1 = LFPulse.ar((carHz) + (carRamp*rampDepth)) * carEnv * amp;
					voice_2 = SinOscFB.ar((carHz*1.5085)+ (carRamp*rampDepth),feedAmp) * carEnv * amp;
					ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);
					voice_1 = (voice_1+(LPF.ar(Impulse.ar(0.003),16000,1)*snap)) * ampMod;
					voice_1 = (voice_1*0.33)+(voice_2*0.33);
					voice_1 = Squiz.ar(in:voice_1, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
					voice_1 = Decimator.ar(voice_1,brate,bcnt,1.0);
					voice_1 = BPeakEQ.ar(in:voice_1,freq:eqHz,rq:1,db:eqAmp,mul:1);
					voice_1 = BLowPass.ar(in:voice_1,freq:lpHz, rq: filterQ, mul:1);
					voice_1 = RHPF.ar(in:voice_1,freq:hpHz, rq: filterQ, mul:1);
					voice_1 = Compander.ar(in:voice_1,control:voice_1, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

					Out.ar(0, Pan2.ar(voice_1,pan));
					FreeSelf.kr(Done.kr(carEnv) * Done.kr(modEnv));
				}).send;

				synthDefs[\hh] = SynthDef.new(\kildare_hh, {
					arg stopGate = 1,
					amp,	carHz, carAtk, carRel,
					tremDepth, tremHz,
					modAmp, modHz, modAtk, modRel,
					feedAmp,
					amDepth, amHz,
					eqHz, eqAmp,
					brate, bcnt,
					lpHz, hpHz, filterQ,
					pan,
					squishPitch, squishChunk;

					var car, mod, carEnv, modEnv, carRamp, tremolo, tremod,
					ampMod;

					amp = amp*0.85;
					lpHz = lpHz.lag3(1);
					hpHz = hpHz.lag3(1);
					pan = pan.lag2(0.1);

					filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
					modAmp = LinLin.kr(modAmp,0.0,1.0,0,127);
					feedAmp = LinLin.kr(feedAmp,0.0,1.0,0.0,10.0);
					eqAmp = LinLin.kr(eqAmp,0.0,1.0,0.0,10.0);
					tremDepth = LinLin.kr(tremDepth,0.0,100,0.0,1.0);
					amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

					modEnv = EnvGen.kr(Env.perc(modAtk, modRel));
					carRamp = EnvGen.kr(Env([1000, 0.000001], [tremHz], curve: \exp));
					carEnv = EnvGen.kr(Env.perc(carAtk, carRel), gate: stopGate, doneAction:2);
					ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);
					mod = SinOsc.ar(modHz, mul:modAmp) * modEnv;
					car = SinOscFB.ar(carHz + mod, feedAmp) * carEnv * amp;
					car = HPF.ar(car,1100,1);
					car = car*ampMod;
					tremolo = SinOsc.ar(tremHz, 0, tremDepth);
					tremod = (1.0 - tremDepth) + tremolo;
					car = car*tremod;
					car = Squiz.ar(in:car, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
					car = Decimator.ar(car,brate,bcnt,1.0);
					car = BPeakEQ.ar(in:car,freq:eqHz,rq:1,db:eqAmp,mul:1);
					car = BLowPass.ar(in:car,freq:lpHz, rq: filterQ, mul:1);
					car = RHPF.ar(in:car,freq:hpHz, rq: filterQ, mul:1);
					car = Compander.ar(in:car,control:car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

					Out.ar(0, Pan2.ar(car,pan));
				}).send;

			} // Server.waitForBoot
		} // StartUp
	} // *initClass

	*new {
		^super.new.init;
	}

	init {
		var s = Server.default;

		topGroup = Group.new(s);
		groups = Dictionary.new;
		sendKeys = Dictionary.new;
		sendKeys = voiceKeys;
		voiceKeys.do({ arg voiceKey;
			groups[voiceKey] = Group.new(topGroup);
		});

		paramProtos = Dictionary.newFrom([
			\bd, Dictionary.newFrom([
				\amp,0.7,
				\carHz,55,
				\carAtk,0,
				\carRel,0.3,
				\modAmp,0,
				\modHz,600,
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
				\brate,24000,
				\bcnt,24,
				\lpHz,19000,
				\hpHz,0,
				\filterQ,50,
				\pan,0,
			]),
			\sd, Dictionary.newFrom([
				\amp,0.7,
				\carHz,282.54,
				\carAtk,0,
				\carRel,0.15,
				\modAmp,0,
				\modHz,2770,
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
				\brate,24000,
				\bcnt,24,
				\lpHz,24000,
				\hpHz,0,
				\filterQ,50,
				\pan,0,
			]),
			\tm, Dictionary.newFrom([
				\amp,0.7,
				\carHz,87.3,
				\carAtk,0,
				\carRel,0.43,
				\modAmp,0.32,
				\modHz,180,
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
				\brate,24000,
				\bcnt,24,
				\click,1,
				\lpHz,24000,
				\hpHz,20,
				\filterQ,50,
				\pan,0,
			]),
			\cp, Dictionary.newFrom([
				\amp,0.7,
				\carHz,1600,
				\carRel,0.43,
				\modAmp,1,
				\modHz,300,
				\modRel,0.5,
				\feedAmp,1,
				\click,0,
				\squishPitch,1,
				\squishChunk,1,
				\amDepth,0,
				\amHz,2698.8,
				\eqHz,6000,
				\eqAmp,0,
				\brate,24000,
				\bcnt,24,
				\click,1,
				\lpHz,24000,
				\hpHz,20,
				\filterQ,50,
				\pan,0,
			]),
			\rs, Dictionary.newFrom([
				\amp,0.7,
				\carHz,370,
				\carAtk,0,
				\carRel,0.05,
				\modAmp,1,
				\modHz,4000,
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
				\brate,24000,
				\bcnt,24,
				\lpHz,19000,
				\hpHz,20,
				\filterQ,50,
				\pan,0,
			]),
			\cb, Dictionary.newFrom([
				\amp,0.7,
				\carHz,404,
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
				\brate,24000,
				\bcnt,24,
				\lpHz,24000,
				\hpHz,20,
				\filterQ,50,
				\pan,0,
			]),
			\hh, Dictionary.newFrom([
				\amp,0.7,
				\carHz,200,
				\carAtk,0,
				\carRel,0.03,
				\modAmp,1,
				\modHz,100,
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
				\brate,24000,
				\bcnt,24,
				\lpHz,19000,
				\hpHz,20,
				\filterQ,50,
				\pan,0,
			]),
		]);
	}

	trigger { arg voiceKey;
		groups[voiceKey].set(\stopGate, -1.05);
		// [EB] added the synthdef name prefix
		// [EB] fix this with `.getPairs` on the dict
		Synth.new(\kildare_++voiceKey, paramProtos[voiceKey].getPairs, groups[voiceKey]);
	}

	setVoiceParam { arg voiceKey, paramKey, paramValue;
		groups[voiceKey].set(paramKey, paramValue);
		paramProtos[voiceKey][paramKey] = paramValue;
	}

	// [EB] added
	allNotesOff {
		topGroup.set(\stopGate, 0);
	}

	free {
		// [EB] added
		topGroup.free;
	}

}