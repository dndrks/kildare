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
	var <feedbackSynths;
	var <busses;
	var <delayBufs;
	var <delayParams;
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
				KildareSaw.new(Crone.server);
				KildareFLD.new(Crone.server);
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
		feedbackSynths = Dictionary.new;

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
		delayBufs[\left1] = Buffer.alloc(s, s.sampleRate * 24.0, 2);
		delayBufs[\right] = Buffer.alloc(s, s.sampleRate * 24.0, 2);

		busses = Dictionary.new;
		busses[\mainOut] = Bus.audio(s, 2);
        busses[\feedbackSend] = Bus.audio(s, 2);

		busses[\out1] = Bus.audio(s,2);
		busses[\out2] = Bus.audio(s,2);
		busses[\out3] = Bus.audio(s,2);
		busses[\feedback1] = Bus.audio(s,2);
		busses[\feedback2] = Bus.audio(s,2);
		busses[\feedback3] = Bus.audio(s,2);
		busses[\feedbackSend1] = Bus.audio(s,2);
		busses[\feedbackSend2] = Bus.audio(s,2);
		busses[\feedbackSend3] = Bus.audio(s,2);
		~feedback = Group.new(addAction:'addToTail');
		~mixer = Group.new(~feedback, \addAfter);
		~processing = Group.new(~mixer, \addAfter);
		~main = Group.new(~processing, \addAfter);

		// feedback matrix
		// based on WazMatrix Mixer V.1, by scacinto

		// ** inFeedback
		SynthDef("feedback1", {
			var in = InFeedback.ar(busses[\feedback1],2);
			in = LeakDC.ar(in);
			in = Limiter.ar(in,0.25);
			Out.ar(busses[\feedbackSend1], in);
		}).add;

		SynthDef("feedback2", {
			var in = InFeedback.ar(busses[\feedback2],2);
			in = LeakDC.ar(in);
			in = Limiter.ar(in,0.25);
			Out.ar(busses[\feedbackSend2], in);
		}).add;

		SynthDef("feedback3", {
			var in = InFeedback.ar(busses[\feedback3],2);
			in = LeakDC.ar(in);
			in = Limiter.ar(in,0.25);
			Out.ar(busses[\feedbackSend3], in);
		}).add;

		// ** MAIN OUT
		SynthDef("mainMixer", {
			arg inA = 0, inB = 0, inC = 0,
			mixSpread = 1, mixCenter = 0, mixLevel = 1;
			var outa, outb, outc, sound;

			outa = Mix.ar(In.ar(busses[\feedback1], 2) * inA);
			outb = Mix.ar(In.ar(busses[\feedback2], 2) * inB);
			outc = Mix.ar(In.ar(busses[\feedback3], 2) * inC);

			sound = Limiter.ar(Splay.ar([outa, outb, outc],spread: mixSpread, level: mixLevel, center: mixCenter), 0.25);

			sound = LeakDC.ar(sound);

			Out.ar(busses[\mainOut], sound);
		}).add;

		// ** CHANNEL MIXERS
		SynthDef("input1Mixer", {
			arg inAmp = 1, outAmp = 1, inA = 0, inB = 0, inC = 0, outA = 1, outB = 0, outC = 0;
			var in1Src, sound, in1A, in1B, in1C, mix, out1, out2, out3;

			in1Src = In.ar(busses[\feedbackSend],2) * inAmp;

			in1A = In.ar(busses[\feedbackSend1], 2);
			in1B = In.ar(busses[\feedbackSend2], 2);
			in1C = In.ar(busses[\feedbackSend3], 2);

			in1A = in1A * inA;
			in1B = in1B * inB;
			in1C = in1C * inC;

			mix = Mix([in1Src, in1A, in1B, in1C]).clip;

			Out.ar(busses[\out1], mix * outA * outAmp);
			Out.ar(busses[\out2], mix * outB * outAmp);
			Out.ar(busses[\out3], mix * outC * outAmp);

		}).add;

		SynthDef("input2Mixer", {
			arg inAmp = 1, outAmp = 1, inA = 0, inB = 0, inC = 0, outA = 0, outB = 1, outC = 0;
			var in2Src, sound, in2A, in2B, in2C, mix, out1, out2, out3;

			in2Src = In.ar(busses[\feedbackSend],2) * inAmp;

			in2A = In.ar(busses[\feedbackSend1], 2);
			in2B = In.ar(busses[\feedbackSend2], 2);
			in2C = In.ar(busses[\feedbackSend3], 2);

			in2A = in2A * inA;
			in2B = in2B * inB;
			in2C = in2C * inC;

			mix = Mix([in2Src, in2A, in2B, in2C]).clip;

			out1 = Out.ar(busses[\out1], mix * outA * outAmp);
			out2 = Out.ar(busses[\out2], mix * outB * outAmp);
			out3 = Out.ar(busses[\out3], mix * outC * outAmp);

		}).add;

		SynthDef("input3Mixer", {
			arg inAmp = 1, outAmp = 1, inA = 0, inB = 0, inC = 0, outA = 0, outB = 0, outC = 1;
			var in3Src, sound, in3A, in3B, in3C, mix, out1, out2, out3;

			in3Src = In.ar(busses[\feedbackSend],2) * inAmp;

			in3A = In.ar(busses[\feedbackSend1], 2);
			in3B = In.ar(busses[\feedbackSend2], 2);
			in3C = In.ar(busses[\feedbackSend3], 2);

			in3A = in3A * inA;
			in3B = in3B * inB;
			in3C = in3C * inC;

			mix = Mix([in3Src, in3A, in3B, in3C]).clip;

			out1 = Out.ar(busses[\out1], mix * outA * outAmp);
			out2 = Out.ar(busses[\out2], mix * outB * outAmp);
			out3 = Out.ar(busses[\out3], mix * outC * outAmp);

		}).add;

		//** PROCESSORS
		SynthDef("processA", {
			arg delayTime = 0.1, delayAmp = 1,
			shiftFreq = 0,
			lSHz = 600, lSdb = 0.0, lSQ = 50,
			hSHz = 19000, hSdb = 0.0, hSQ = 50,
			eqHz = 6000, eqdb = 0.0, eqQ = 0.0;
			var in, sound;

			in = In.ar(busses[\out1],2);

			lSHz = lSHz.lag3(0.1);
			hSHz = hSHz.lag3(0.1);
			eqHz = eqHz.lag3(0.1);
			lSdb = lSdb.lag3(0.1);
			hSdb = hSdb.lag3(0.1);
			eqdb = eqdb.lag3(0.1);
			lSQ = LinLin.kr(lSQ,0,100,1.0,0.3);
			hSQ = LinLin.kr(hSQ,0,100,1.0,0.3);
			eqQ = LinLin.kr(eqQ,0,100,1.0,0.1);

			sound = DelayC.ar(in, 3, delayTime, delayAmp);
			sound = FreqShift.ar(sound, shiftFreq);
			sound = BLowShelf.ar(sound, lSHz, lSQ, lSdb);
			sound = BHiShelf.ar(sound, hSHz, hSQ, hSdb);

			Out.ar(busses[\feedback1], sound.tan);
		}).add;

		SynthDef("processB", {
			arg delayTime = 0.1, delayAmp = 1,
			shiftFreq = 0,
			lSHz = 600, lSdb = 0.0, lSQ = 50,
			hSHz = 19000, hSdb = 0.0, hSQ = 50,
			eqHz = 6000, eqdb = 0.0, eqQ = 0.0;
			var in, sound;

			in = In.ar(busses[\out2],2);

			lSHz = lSHz.lag3(0.1);
			hSHz = hSHz.lag3(0.1);
			eqHz = eqHz.lag3(0.1);
			lSdb = lSdb.lag3(0.1);
			hSdb = hSdb.lag3(0.1);
			eqdb = eqdb.lag3(0.1);
			lSQ = LinLin.kr(lSQ,0,100,1.0,0.3);
			hSQ = LinLin.kr(hSQ,0,100,1.0,0.3);
			eqQ = LinLin.kr(eqQ,0,100,1.0,0.1);

			sound = DelayC.ar(in, 3, delayTime, delayAmp);
			sound = FreqShift.ar(sound, shiftFreq);
			sound = BLowShelf.ar(sound, lSHz, lSQ, lSdb);
			sound = BHiShelf.ar(sound, hSHz, hSQ, hSdb);

			Out.ar(busses[\feedback2], sound.tan);
		}).add;

		SynthDef("processC", {
			arg delayTime = 0.1, delayAmp = 1,
			shiftFreq = 0,
			lSHz = 600, lSdb = 0.0, lSQ = 50,
			hSHz = 19000, hSdb = 0.0, hSQ = 50,
			eqHz = 6000, eqdb = 0.0, eqQ = 0.0;
			var in, sound;

			in = In.ar(busses[\out3],2);

			lSHz = lSHz.lag3(0.1);
			hSHz = hSHz.lag3(0.1);
			eqHz = eqHz.lag3(0.1);
			lSdb = lSdb.lag3(0.1);
			hSdb = hSdb.lag3(0.1);
			eqdb = eqdb.lag3(0.1);
			lSQ = LinLin.kr(lSQ,0,100,1.0,0.3);
			hSQ = LinLin.kr(hSQ,0,100,1.0,0.3);
			eqQ = LinLin.kr(eqQ,0,100,1.0,0.1);

			sound = DelayC.ar(in, 3, delayTime, delayAmp);
			sound = FreqShift.ar(sound, shiftFreq);
			sound = BLowShelf.ar(sound, lSHz, lSQ, lSdb);
			sound = BHiShelf.ar(sound, hSHz, hSQ, hSdb);

			Out.ar(busses[\feedback3], sound.tan);
		}).add;

		s.sync;

		feedbackSynths[\aFeedback] = Synth("feedback1", target: ~feedback);
		feedbackSynths[\bFeedback] = Synth("feedback2", target: ~feedback);
		feedbackSynths[\cFeedback] = Synth("feedback3", target: ~feedback);
		feedbackSynths[\aMixer] = Synth(\input1Mixer, target: ~mixer);
		feedbackSynths[\bMixer] = Synth(\input2Mixer, target: ~mixer);
		feedbackSynths[\cMixer] = Synth(\input3Mixer, target: ~mixer);
		feedbackSynths[\aProcess] = Synth(\processA, target: ~processing);
		feedbackSynths[\bProcess] = Synth(\processB, target: ~processing);
		feedbackSynths[\cProcess] = Synth(\processC, target: ~processing);
		feedbackSynths[\mainMixer] = Synth(\mainMixer, target: ~main);
		// \ feedback

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
			\feedbackSend, 0
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
			\level, 1,
			\limiterLevel, 0.5
		]);

		generalizedParams = Dictionary.newFrom([
			\kildare_bd, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\feedbackAux,busses[\feedbackSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\feedbackSend,0,
				\poly,0,
				\velocity,127,
				\amp,0.7,
				\carHz,55,
				\thirdHz,55,
				\seventhHz,55,
				\carDetune,0,
				\carAtk,0,
				\carRel,0.3,
				\carCurve,-4,
				\modAmp,0,
				\modHz,600,
				\modFollow,0,
				\modNum,1,
				\modDenum,1,
				\modAtk,0,
				\modRel,0.05,
				\modCurve,-4,
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
				\lpHz,20000,
				\hpHz,20,
				\filterQ,50,
				\lpAtk,0,
				\lpRel,0.3,
				\lpCurve,-4,
				\lpDepth,0,
				\pan,0,
			]),
			\kildare_sd, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\feedbackAux,busses[\feedbackSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\feedbackSend,0,
				\poly,0,
				\amp,0.7,
				\carHz,282.54,
				\thirdHz,282.54,
				\seventhHz,282.54,
				\carDetune,0,
				\carAtk,0,
				\carRel,0.15,
				\carCurve,-4,
				\modAmp,0,
				\modHz,2770,
				\modFollow,0,
				\modNum,1,
				\modDenum,1,
				\modAtk,0.2,
				\modRel,1,
				\modCurve,-4,
				\feedAmp,0,
				\noiseAmp,0.01,
				\noiseAtk,0,
				\noiseRel,0.1,
				\noiseCurve,-4,
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
				\lpAtk,0,
				\lpRel,0.3,
				\lpCurve,-4,
				\lpDepth,0,
				\pan,0,
			]),
			\kildare_tm, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\feedbackAux,busses[\feedbackSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\feedbackSend,0,
				\poly,0,
				\amp,0.7,
				\carHz,87.3,
				\carDetune,0,
				\carAtk,0,
				\carRel,0.43,
				\carCurve,-4,
				\modAmp,0.32,
				\modHz,180,
				\modFollow,0,
				\modNum,1,
				\modDenum,1,
				\modAtk,0,
				\modRel,0.2,
				\modCurve,-4,
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
				\feedbackAux,busses[\feedbackSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\feedbackSend,0,
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
				\feedbackAux,busses[\feedbackSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\feedbackSend,0,
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
				\feedbackAux,busses[\feedbackSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\feedbackSend,0,
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
				\feedbackAux,busses[\feedbackSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\feedbackSend,0,
				\poly,0,
				\amp,0.7,
				\carHz,200,
				\thirdHz,200,
				\seventhHz,200,
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
				\feedbackAux,busses[\feedbackSend],
				\delayEnv,0,
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\feedbackSend,0,
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
			]),
			\kildare_saw, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\feedbackAux,busses[\feedbackSend],
				\delayAtk,0,
				\delayRel,2,
				\delaySend,0,
				\feedbackSend,0,
				\poly,0,
				\velocity,127,
				\amp,0.7,
				\carHz,55,
				\thirdHz,55,
				\seventhHz,55,
				\subSqAmp,1,
				\subSqPW,0.5,
				\subSqPWMRate,0.03,
				\subSqPWMAmt,0,
				\phaseOff1,2/3,
				\phaseOff2,4/3,
				\carDetune,0,
				\carAtk,0,
				\carRel,0.3,
				\carCurve,-4,
				\modAmp,0,
				\modHz,600,
				\modFollow,0,
				\modNum,1,
				\modDenum,1,
				\modAtk,0,
				\modRel,0.05,
				\modCurve,-4,
				\feedAmp,1,
				\rampDepth,0.0,
				\rampDec,0.3,
				\squishPitch,1,
				\squishChunk,1,
				\amDepth,0,
				\amHz,8175.08,
				\eqHz,6000,
				\eqAmp,0,
				\bitRate,24000,
				\bitCount,24,
				\lpHz,20000,
				\hpHz,10,
				\filterQ,50,
				\lpAtk,0,
				\lpRel,0.3,
				\lpCurve,-4,
				\lpDepth,0,
				\pan,0,
			]),
			\kildare_fld, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\feedbackAux,busses[\feedbackSend],
				\delaySend,0,
				\delayEnv,0,
				\delayAtk,0,
				\delayRel,2,
				\feedbackSend,0,
				\poly,0,
				\velocity,127,
				\amp,0.7,
				\carHz,55,
				\thirdHz,55,
				\seventhHz,55,
				\carDetune,0,
				\carAtk,0,
				\carRel,3.3,
				\carCurve,-4,
				\modAmp,0,
				\modHz,600,
				\modFollow,0,
				\modNum,1,
				\modDenum,1,
				\modAtk,0,
				\modRel,0.05,
				\modCurve,-4,
				\feedAmp,1,
				\rampDepth,0,
				\rampDec,0.3,
				\squishPitch,1,
				\squishChunk,1,
				\amDepth,0,
				\amHz,8175.08,
				\eqHz,6000,
				\eqAmp,0,
				\bitRate,24000,
				\bitCount,24,
				\lpHz,20000,
				\hpHz,20,
				\filterQ,50,
				\lpAtk,0,
				\lpRel,0.3,
				\lpCurve,-4,
				\lpDepth,0,
				\pan,0,
			]),
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
			feedbackSend = 0,
			inputL, inputR,
			mainOutput, feedbackOutput;

			var delayL, delayR,
			localin, del, input;

			time = time.lag3(0.2);
			feedback = feedback.lag3(0.1);
			lpHz = lpHz.lag3(0.05);
			hpHz = hpHz.lag3(0.05);
			level = level.lag3(0.1);

			input = In.ar(inputL,2); // TODO GENERALIZE IN SYNTHS, SHOULDN'T MATTER WHICH PANNING...
			localin = LocalIn.ar(2);

			filterQ = LinLin.kr(filterQ,0,100,1.0,0.001);

			// thank you ezra for https://github.com/catfact/engine-intro/blob/master/EngineIntro_NoiseSine.sc#L35-L49
			delayL = BufDelayC.ar(delayBufs[\left1].bufnum, input[0] + (feedback * localin[1]), time, 1);
			delayR = BufDelayC.ar(delayBufs[\right].bufnum, (feedback * localin[0]), time, 1);

			del = [delayL, delayR];
			LocalOut.ar(del);

			del = Splay.ar(del,spread,1);
			del = RLPF.ar(in:del, freq:lpHz, rq: filterQ, mul:1);
			del = RHPF.ar(in:del, freq:hpHz, rq: filterQ, mul:1);
			del = Balance2.ar(del[0],del[1],pan);

			Out.ar(mainOutput, del * level); // level down here, so the delays continue
			Out.ar(feedbackOutput,del * level * feedbackSend);

        }).play(target:s, addAction:\addToTail, args:[
			\inputL, busses[\delayLSend],
			\inputR, busses[\delayRSend],
			\feedbackOutput, busses[\feedbackSend],
			\mainOutput, busses[\mainOut]
        ]);

        outputSynths[\main] = SynthDef.new(\main, {
            arg in, out,
			lSHz = 600, lSdb = 0.0, lSQ = 50,
			hSHz = 19000, hSdb = 0.0, hSQ = 50,
			eqHz = 6000, eqdb = 0.0, eqQ = 0.0,
			level = 1.0, limiterLevel = 0.5;
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
			src = Limiter.ar(src, limiterLevel);

			Out.ar(out, src * level);
        }).play(target:s, addAction:\addToTail, args: [
            \in, busses[\mainOut], \out, 0
        ]);

	}

	trigger { arg voiceKey, velocity, retrigFlag;
		paramProtos[voiceKey][\velocity] = velocity;
		if( paramProtos[voiceKey][\poly] == 0,{
			indexTracker[voiceKey] = numVoices;
			if( retrigFlag == 'true',{
				if ((""++synthKeys[voiceKey]++"").contains("sample"), {
				},{
					groups[voiceKey].set(\stopGate, -1.05);
				});
			},{
				groups[voiceKey].set(\stopGate, -1.1);
			});
			if ((""++synthKeys[voiceKey]++"").contains("sample"), {
				// (retrigFlag).postln;
				if( retrigFlag == 'true',{
					// groups[voiceKey].set(\t_trig, -1.025);
					// ('re-triggering').postln;
					groups[voiceKey].set(\t_trig, 1);
				},{
					groups[voiceKey].set(\t_trig, -1.05);
					Synth.new(\kildare_sample, paramProtos[voiceKey].getPairs, groups[voiceKey]);
					// ('fresh trigger').postln;
				});
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

	setFeedbackParam { arg targetKey, paramKey, paramValue;
		// (targetKey).postln;
		// (paramKey).postln;
		// (paramValue).postln;
		feedbackSynths[(targetKey).asSymbol].set(paramKey, paramValue);
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
			paramProtos[voice][\channels] = sampleInfo[voice][\samples][samplenum].numChannels;
			paramProtos[voice][\channels].postln;
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
		~feedback.free;
		~input.free;
		~mixer.free;
		~processing.free;
		~main.free;
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
		feedbackSynths.do({arg bus;
			bus.free;
		});
		delayBufs.do({arg buf;
			buf.free;
		});
		Buffer.freeAll(Crone.server);
	}

}