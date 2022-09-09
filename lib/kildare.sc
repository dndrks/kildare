Kildare {
	classvar <voiceKeys;
	classvar <synthDefs;
	classvar <synthKeys;
	const <numVoices = 3;

	var <paramProtos;
	var <generalizedParams;
	var <groups;
	var <topGroup;

	var <outputSynths;
	var <busses;
	var <delayBufs;
	var <delayParams;
	var <reverbParams;
	var <mainOutParams;

	var <voiceTracker;
	var <folderedSamples;
	classvar <sampleInfo;
	classvar <indexTracker;

	*initClass {
		voiceKeys = [ \1, \2, \3, \4, \5, \6, \7, \sample1, \sample2, \sample3];

		StartUp.add {
			var s = Server.default;

			s.waitForBoot {
				synthDefs = Dictionary.new;

				synthKeys = Dictionary.newFrom([
					\1, \kildare_bd,
					\2, \kildare_sd,
					\3, \kildare_tm,
					\4, \kildare_cp,
					\5, \kildare_rs,
					\6, \kildare_cb,
					\7, \kildare_hh,
					\sample1, \kildare_sample,
					\sample2, \kildare_sample,
					\sample3, \kildare_sample,
				]);

				synthDefs[\1] = KildareBD.new(Crone.server);
				synthDefs[\2] = KildareSD.new(Crone.server);
				synthDefs[\3] = KildareTM.new(Crone.server);
				synthDefs[\4] = KildareCP.new(Crone.server);
				synthDefs[\5] = KildareRS.new(Crone.server);
				synthDefs[\6] = KildareCB.new(Crone.server);
				synthDefs[\7] = KildareHH.new(Crone.server);
				synthDefs[\sample1] = KildareSample.new(Crone.server);
				synthDefs[\sample2] = KildareSample.new(Crone.server);
				synthDefs[\sample3] = KildareSample.new(Crone.server);

			} // Server.waitForBoot
		} // StartUp
	} // *initClass

	*new {
		^super.new.init;
	}

	init {
		var s = Server.default, sample_iterator = 1;

		outputSynths = Dictionary.new;

		voiceTracker = Dictionary.new;
		indexTracker = Dictionary.new;

		folderedSamples = Dictionary.new;
		sampleInfo = Dictionary.newFrom([
			\sample1, Dictionary.newFrom(
				[
					\samples, Dictionary.new(),
					\pointers, Dictionary.new(),
					\samplerates, Dictionary.new()
			]),
			\sample2, Dictionary.newFrom(
				[
					\samples, Dictionary.new(),
					\pointers, Dictionary.new(),
					\samplerates, Dictionary.new()
			]),
			\sample3, Dictionary.newFrom(
				[
					\samples, Dictionary.new(),
					\pointers, Dictionary.new(),
					\samplerates, Dictionary.new()
			]),
		]);

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
			\pan, 0,
			\lpHz, 20000,
			\hpHz, 20,
			\filterQ, 50,
			\reverbSend, 0
		]);

		reverbParams = Dictionary.newFrom([
			\preDelay, 0,
			\level, 1,
			\decay, 2,
			\refAmp, 0,
			\refOffset, 0,
			\refDiv, 10,
			\modFreq, 0.1,
			\modDepth, 0,
			\highCut, 8000,
			\lowCut, 10,
			\thresh, 0,
			\slopeBelow, 1,
			\slopeAbove, 1,
			\clampTime, 0.01,
			\relaxTime,0.1
		]);

		mainOutParams = Dictionary.newFrom([
			\lSHz, 600,
			\lSdb, 0,
			\lSQ, 50,
			\hSHz, 19000,
			\hSdb, 0,
			\hSQ, 50,
			\eqHz, 6000,
			\eqdb, 0,
			\eqQ, 50,
			\level, 1
		]);

		generalizedParams = Dictionary.newFrom([
			\kildare_bd, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\reverbAux,busses[\reverbSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\reverbSend,0,
				\poly,0,
				\velocity,127,
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
			\kildare_sd, Dictionary.newFrom([
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
			\kildare_tm, Dictionary.newFrom([
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
			\kildare_cp, Dictionary.newFrom([
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
			\kildare_rs, Dictionary.newFrom([
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
			\kildare_cb, Dictionary.newFrom([
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
			\kildare_hh, Dictionary.newFrom([
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
			\kildare_sample, Dictionary.newFrom([
				\bufnum,nil,
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\reverbAux,busses[\reverbSend],
				\delayEnv,0,
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\reverbSend,0,
				\poly,0,
				\amp,1,
				\sampleEnv,0,
				\sampleAtk,0,
				\sampleRel,1,
				\sampleStart,0,
				\sampleEnd,1,
				\loop,0,
				\rate,1,
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
				\t_trig,1,
				\startA, 0,
				\startB, 0,
				\crossfade, 0,
				\aOrB, 0
			])
		]);

		paramProtos = Dictionary.newFrom([
			\1, Dictionary.newFrom(generalizedParams[\kildare_bd]),
			\2, Dictionary.newFrom(generalizedParams[\kildare_sd]),
			\3, Dictionary.newFrom(generalizedParams[\kildare_tm]),
			\4, Dictionary.newFrom(generalizedParams[\kildare_cp]),
			\5, Dictionary.newFrom(generalizedParams[\kildare_rs]),
			\6, Dictionary.newFrom(generalizedParams[\kildare_cb]),
			\7, Dictionary.newFrom(generalizedParams[\kildare_hh]),
			\sample1, Dictionary.newFrom(generalizedParams[\kildare_sample]),
			\sample2, Dictionary.newFrom(generalizedParams[\kildare_sample]),
			\sample3, Dictionary.newFrom(generalizedParams[\kildare_sample]),
		]);

		outputSynths[\delay] = SynthDef.new(\delay, {

			arg time = 0.3, level = 1.0, feedback = 0.7,
			lpHz = 20000, hpHz = 20, filterQ = 50,
			spread = 1, pan = 0,
			reverbSend = 0,
			inputL, inputR,
			mainOutput, reverbOutput;

			var delayL, delayR,
			leftBal, rightBal,
			leftInput, rightInput, startHit,
			leftPos, rightPos,
			feedbackDecayTime;

			time = time.lag3(0.2);
			feedback = feedback.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			level = level.lag3(0.1);

			leftInput = In.ar(inputL, 1);
			rightInput = In.ar(inputR, 1);

			filterQ = LinLin.kr(filterQ,0,100,1.0,0.001);
			leftPos = LinLin.kr(spread,0,1,0,-1);
			rightPos = LinLin.kr(spread,0,1,0,1);

			feedbackDecayTime = (0.001.log * time) / feedback.log;

			startHit = BufCombC.ar(delayBufs[\initHit].bufnum,leftInput,time/2,0,level);
			delayL = BufCombC.ar(delayBufs[\left1].bufnum,leftInput,time/2,0,level);
			delayL = BufCombC.ar(delayBufs[\left2].bufnum,delayL,time,feedbackDecayTime,level);
			delayR = BufCombC.ar(delayBufs[\right].bufnum,rightInput,time,feedbackDecayTime*0.9,level);

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

			arg preDelay = 0.0, level = 1, decay = 2,
			refAmp = 0.0, refDiv = 10, refOffset = 0, modDepth = 0.0, modFreq = 0.1,
			highCut = 8000, lowCut = 10,
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
						delaytime: (decTime - (decTime*(voice/refDiv))) + (refOffset/100),
						decaytime: decay,
						mul: (1/5) * level * refAmp
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
			lSHz = 600, lSdb = 0.0, lSQ = 50,
			hSHz = 19000, hSdb = 0.0, hSQ = 50,
			eqHz = 6000, eqdb = 0.0, eqQ = 0.0,
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

	trigger { arg voiceKey, velocity;
		paramProtos[voiceKey][\velocity] = velocity;
		if( paramProtos[voiceKey][\poly] == 0,{
			indexTracker[voiceKey] = numVoices;
			groups[voiceKey].set(\stopGate, -1.1);
			if ((""++synthKeys[voiceKey]++"").contains("sample"), {
				groups[voiceKey].set(\t_trig, -1.05);
				Synth.new(\kildare_sample, paramProtos[voiceKey].getPairs, groups[voiceKey]);
			},{
				Synth.new(synthKeys[voiceKey], paramProtos[voiceKey].getPairs, groups[voiceKey]);
			});
		},{
			indexTracker[voiceKey] = (indexTracker[voiceKey] + 1)%numVoices;
			if (voiceTracker[voiceKey][indexTracker[voiceKey]].isNil.not, {
				if (voiceTracker[voiceKey][indexTracker[voiceKey]].isPlaying, {
					if ((""++synthKeys[voiceKey]++"").contains("sample"), {
						voiceTracker[voiceKey][indexTracker[voiceKey]].set(\t_trig, -1.05);
					},{
						voiceTracker[voiceKey][indexTracker[voiceKey]].set(\stopGate, -1.1);
					});
				});
			});
			if ((""++synthKeys[voiceKey]++"").contains("sample"),{
				voiceTracker[voiceKey][indexTracker[voiceKey]] = Synth.new(\kildare_sample, paramProtos[voiceKey].getPairs, groups[voiceKey]);
			},{
				voiceTracker[voiceKey][indexTracker[voiceKey]] = Synth.new(synthKeys[voiceKey], paramProtos[voiceKey].getPairs, groups[voiceKey]);
			});

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

	clearSamples { arg voice;
		if ( sampleInfo[voice][\samples].size > 0, {
			for ( 0, sampleInfo[voice][\samples].size-1, {
				arg i;
				sampleInfo[voice][\samples][i].free;
				sampleInfo[voice][\samples][i] = nil;
				("freeing buffer "++i).postln;
			});
		});
	}

	loadFile { arg msg;
		var voice = msg[1], filename = msg[2];
		groups[voice].set(\t_trig, -1);
		groups[voice].set(\stopGate, -1);
		this.clearSamples(voice);
		sampleInfo[voice][\samples][0] = Buffer.read(Server.default, filename ,action:{
			arg bufnum;
			sampleInfo[voice][\pointers][0] = bufnum;
			this.setFile(voice,1);
		});
	}

	setFile { arg voice, samplenum;
		if ( sampleInfo[voice][\samples].size > 0, {
			samplenum = samplenum - 1;
			samplenum = samplenum.wrap(0,sampleInfo[voice][\samples].size-1);
			paramProtos[voice][\bufnum] = sampleInfo[voice][\pointers][samplenum];
			sampleInfo[voice][\samplerates][samplenum] = sampleInfo[voice][\samples][samplenum].sampleRate;
		});
	}

	loadFileIntoContainer { arg voice, index, path;
		sampleInfo[voice][\samples][index] = Buffer.read(Server.default, path, action:{
			arg bufnum;
			sampleInfo[voice][\pointers][index] = bufnum;
			sampleInfo[voice][\pointers][index].postln;
			if (index == 0, {
				this.setFile(voice,1);
			});
		});
	}

	loadFolder { arg voice, filepath;
		this.clearSamples(voice);
		folderedSamples[voice] = SoundFile.collect(filepath++"*");
		for ( 0, folderedSamples[voice].size-1, {
			arg i;
			this.loadFileIntoContainer(voice,i,folderedSamples[voice][i].path);
		});
	}

	adjustSampleMult { arg voice, mult;
		if (paramProtos[voice][\rate] != mult, {
			groups[voice].set(\rate, paramProtos[voice][\rate] * mult);
		});
	}

	adjustSampleOffset { arg voice, offset;
		groups[voice].set(\rate, (paramProtos[voice][\rate]) * (0.5**((offset*-1)/12)));
	}

	stopSample { arg voice;
		groups[voice].set(\t_trig, -1.1);
		groups[voice].set(\stopGate, -1.1);
	}

	setModel { arg voice, model;
		synthKeys[voice] = model;
		paramProtos[voice] = Dictionary.newFrom(generalizedParams[model]);
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
		Buffer.freeAll(Crone.server);
	}

}