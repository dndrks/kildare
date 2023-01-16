Kildare {
	classvar <voiceKeys;
	classvar <synthDefs;

	var <synthKeys;
	var <paramProtos;
	var <generalizedParams;
	var <polyParamStyle;
	var <polyParams;
	var <groups;
	var <topGroup;

	var <outputSynths;
	var <feedbackSynths;
	var <busses;
	var <delayBufs;
	var <delayParams;
	var <mainOutParams;

	var <voiceTracker;
	var <emptyVoices;
	var <folderedSamples;
	var <voiceLimit;
	classvar <sampleInfo;
	classvar <indexTracker;

	*initClass {
		voiceKeys = [ \1, \2, \3, \4, \5, \6, \7, \8];

		StartUp.add {
			var s = Server.default;

			s.waitForBoot {
				synthDefs = Dictionary.new;

				synthDefs[\bd] = KildareBD.new(Crone.server);
				synthDefs[\sd] = KildareSD.new(Crone.server);
				synthDefs[\tm] = KildareTM.new(Crone.server);
				synthDefs[\cp] = KildareCP.new(Crone.server);
				synthDefs[\rs] = KildareRS.new(Crone.server);
				synthDefs[\cb] = KildareCB.new(Crone.server);
				synthDefs[\hh] = KildareHH.new(Crone.server);
				synthDefs[\saw] = KildareSaw.new(Crone.server);
				synthDefs[\fld] = KildareFLD.new(Crone.server);
				synthDefs[\timbre] = KildareTimbre.new(Crone.server);
				synthDefs[\ptr] = KildarePTR.new(Crone.server);
				synthDefs[\sample] = KildareSample.new(Crone.server);

			} // Server.waitForBoot
		} // StartUp
	} // *initClass

	*new {
		^super.new.init;
	}

	init {
		var s = Server.default, sample_iterator = 1;

		synthKeys = Dictionary.newFrom([
			\1, \none,
			\2, \none,
			\3, \none,
			\4, \none,
			\5, \none,
			\6, \none,
			\7, \none,
			\8, \none,
		]);

		outputSynths = Dictionary.new;
		feedbackSynths = Dictionary.new;

		voiceTracker = Dictionary.new;
		voiceLimit = Dictionary.newFrom([
			\1, 4,
			\2, 4,
			\3, 4,
			\4, 4,
			\5, 4,
			\6, 4,
			\7, 4,
			\8, 4,
		]);
		polyParams = Dictionary.new(voiceKeys.size);
		polyParamStyle = Dictionary.newFrom([
			\1, "all voices",
			\2, "all voices",
			\3, "all voices",
			\4, "all voices",
			\5, "all voices",
			\6, "all voices",
			\7, "all voices",
			\8, "all voices",
		]);
		emptyVoices = Dictionary.new;
		(1..8).do{arg i; emptyVoices[i] = false};
		indexTracker = Dictionary.new;

		folderedSamples = Dictionary.new;
		sampleInfo = Dictionary.newFrom([
			\1, Dictionary.newFrom(
				[
					\samples, Dictionary.new(),
					\pointers, Dictionary.new(),
					\samplerates, Dictionary.new()
			]),
			\2, Dictionary.newFrom(
				[
					\samples, Dictionary.new(),
					\pointers, Dictionary.new(),
					\samplerates, Dictionary.new()
			]),
			\3, Dictionary.newFrom(
				[
					\samples, Dictionary.new(),
					\pointers, Dictionary.new(),
					\samplerates, Dictionary.new()
			]),
			\4, Dictionary.newFrom(
				[
					\samples, Dictionary.new(),
					\pointers, Dictionary.new(),
					\samplerates, Dictionary.new()
			]),
			\5, Dictionary.newFrom(
				[
					\samples, Dictionary.new(),
					\pointers, Dictionary.new(),
					\samplerates, Dictionary.new()
			]),
			\6, Dictionary.newFrom(
				[
					\samples, Dictionary.new(),
					\pointers, Dictionary.new(),
					\samplerates, Dictionary.new()
			]),
			\7, Dictionary.newFrom(
				[
					\samples, Dictionary.new(),
					\pointers, Dictionary.new(),
					\samplerates, Dictionary.new()
			]),
			\8, Dictionary.newFrom(
				[
					\samples, Dictionary.new(),
					\pointers, Dictionary.new(),
					\samplerates, Dictionary.new()
			]),
		]);

		groups = Dictionary.new;
		voiceKeys.do({ arg voiceKey;
			indexTracker[voiceKey] = 0;
			polyParams[voiceKey] = Dictionary.new(8);
			8.do{ arg i;
				voiceTracker[voiceKey] = Dictionary.new(8);
				polyParams[voiceKey][i] = Dictionary.new;
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
			mixSpread = 1, mixCenter = 0, mixLevel = 1,
			lSHz = 600, lSdb = 0.0, lSQ = 50,
			hSHz = 19000, hSdb = 0.0, hSQ = 50;
			var outa, outb, outc, sound;

			outa = Mix.ar(In.ar(busses[\feedback1], 2) * inA);
			outb = Mix.ar(In.ar(busses[\feedback2], 2) * inB);
			outc = Mix.ar(In.ar(busses[\feedback3], 2) * inC);

			sound = Splay.ar([outa, outb, outc],spread: mixSpread, level: mixLevel, center: mixCenter);
			sound = BLowShelf.ar(sound, lSHz, lSQ, lSdb);
			sound = BHiShelf.ar(sound, hSHz, hSQ, hSdb);
			sound = Limiter.ar(sound, 0.25);

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
			delayTime = delayTime.lag3(0.2);
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
			delayTime = delayTime.lag3(0.2);
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
			delayTime = delayTime.lag3(0.2);
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
				\delayEnv,0,
				\delayAtk,0,
				\delayRel,2,
				\delayCurve,-4,
				\delaySend,0,
				\feedbackEnv,0,
				\feedbackAtk,0,
				\feedbackRel,2,
				\feedbackCurve,-4,
				\feedbackSend,0,
				\poly,0,
				\velocity,127,
				\amp,0.7,
				\carHz,55,
				\carHzThird,55,
				\carHzSeventh,55,
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
				\velocity,127,
				\delayAtk,0,
				\delayRel,2,
				\delayCurve,-4,
				\delaySend,0,
				\feedbackEnv,0,
				\feedbackAtk,0,
				\feedbackRel,2,
				\feedbackCurve,-4,
				\feedbackSend,0,
				\poly,0,
				\amp,0.7,
				\carHz,282.54,
				\carHzThird,282.54,
				\carHzSeventh,282.54,
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
				\lpHz,20000,
				\hpHz,20,
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
				\delayCurve,-4,
				\delaySend,0,
				\feedbackEnv,0,
				\feedbackAtk,0,
				\feedbackRel,2,
				\feedbackCurve,-4,
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
				\delayCurve,-4,
				\delaySend,0,
				\feedbackEnv,0,
				\feedbackAtk,0,
				\feedbackRel,2,
				\feedbackCurve,-4,
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
				\delayCurve,-4,
				\delaySend,0,
				\feedbackEnv,0,
				\feedbackAtk,0,
				\feedbackRel,2,
				\feedbackCurve,-4,
				\feedbackSend,0,
				\poly,0,
				\amp,1.0,
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
				\delayCurve,-4,
				\delaySend,0,
				\feedbackEnv,0,
				\feedbackAtk,0,
				\feedbackRel,2,
				\feedbackCurve,-4,
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
				\delayCurve,-4,
				\delaySend,0,
				\feedbackEnv,0,
				\feedbackAtk,0,
				\feedbackRel,2,
				\feedbackCurve,-4,
				\feedbackSend,0,
				\poly,0,
				\amp,0.7,
				\carHz,200,
				\carHzThird,200,
				\carHzSeventh,200,
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
				\channels,2,
				\delayEnv,0,
				\delayAtk,0,
				\delayRel,2,
				\delayCurve,-4,
				\delaySend,0,
				\feedbackEnv,0,
				\feedbackAtk,0,
				\feedbackRel,2,
				\feedbackCurve,-4,
				\feedbackSend,0,
				\poly,0,
				\amp,1,
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
				\delayCurve,-4,
				\delaySend,0,
				\feedbackEnv,0,
				\feedbackAtk,0,
				\feedbackRel,2,
				\feedbackCurve,-4,
				\feedbackSend,0,
				\poly,0,
				\velocity,127,
				\amp,0.7,
				\carHz,55,
				\carHzThird,55,
				\carHzSeventh,55,
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
				\delayCurve,-4,
				\feedbackEnv,0,
				\feedbackAtk,0,
				\feedbackRel,2,
				\feedbackCurve,-4,
				\feedbackSend,0,
				\poly,0,
				\velocity,127,
				\amp,0.7,
				\carHz,55,
				\carHzThird,55,
				\carHzSeventh,55,
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
			\kildare_timbre, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\feedbackAux,busses[\feedbackSend],
				\delaySend,0,
				\delayEnv,0,
				\delayAtk,0,
				\delayRel,2,
				\delayCurve,-4,
				\feedbackEnv,0,
				\feedbackAtk,0,
				\feedbackRel,2,
				\feedbackCurve,-4,
				\feedbackSend,0,
				\poly,0,
				\velocity,127,
				\amp,0.7,
				\carSawAmp,0.25,
				\carTriAmp,0.25,
				\carPulseAmp,0.25,
				\carSineAmp,0.25,
				\carHz,55,
				\carHzThird,55,
				\carHzSeventh,55,
				\carDetune,0,
				\carAtk,0,
				\carRel,3.3,
				\carCurve,-4,
				\modAmp,0,
				\modSawAmp,0,
				\modTriAmp,0,
				\modPulseAmp,0,
				\modSineAmp,0,
				\modHz,600,
				\modLP,20000,
				\modQ,50,
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
			\kildare_ptr, Dictionary.newFrom([
				\out,busses[\mainOut],
				\delayAuxL,busses[\delayLSend],
				\delayAuxR,busses[\delayRSend],
				\feedbackAux,busses[\feedbackSend],
				\delaySend,0,
				\delayEnv,0,
				\delayAtk,0,
				\delayRel,2,
				\delayCurve,-4,
				\feedbackEnv,0,
				\feedbackAtk,0,
				\feedbackRel,2,
				\feedbackCurve,-4,
				\feedbackSend,0,
				\poly,0,
				\velocity,127,
				\amp,0.7,
				\carHz,55,
				\carHzThird,55,
				\carHzSeventh,55,
				\carDetune,0,
				\carAtk,0,
				\carRel,3.3,
				\carCurve,-4,
				\formantHz,55,
				\formantFollow,0,
				\formantNum,1,
				\formantDenum,1,
				\width,0.5,
				\phaseMul,0,
				\phaseAmp,0,
				\sync,0,
				\phaseAtk,0,
				\phaseRel,0.05,
				\phaseCurve,-4,
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
			\8, Dictionary.newFrom(generalizedParams[\kildare_saw]),
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

	test_trigger { arg voiceKey, velocity, allocVoice;

		paramProtos[voiceKey][\velocity] = velocity;
		if( paramProtos[voiceKey][\poly] == 0,{
			if( groups[voiceKey].isPlaying,{
				groups[voiceKey].set(\velocity, velocity);
				if ((""++synthKeys[voiceKey]++"").contains("sample"), {
					groups[voiceKey].set(\t_trig, 1);
					('triggering sample').postln;
					/*Routine{
						groups[voiceKey].set(\loop_trig,0);
						0.01.wait;
						groups[voiceKey].set(\t_trig, 1);
						groups[voiceKey].set(\loop_trig,1);
					}.play;*/
				});
				groups[voiceKey].set(\t_gate, 1);
			});
		},{
			// indexTracker[voiceKey] = (indexTracker[voiceKey] + 1) % voiceLimit[voiceKey];
			indexTracker[voiceKey] = allocVoice;
			if (voiceTracker[voiceKey][allocVoice].isPlaying, {
				voiceTracker[voiceKey][allocVoice].set(\velocity, velocity);
				if ((""++synthKeys[voiceKey]++"").contains("sample"), {
					voiceTracker[voiceKey][allocVoice].set(\t_trig, 1);
					('triggering poly sample').postln;
				});
				voiceTracker[voiceKey][allocVoice].set(\t_gate, 1);
			});
		});
	}

	/*trigger { arg voiceKey, velocity, retrigFlag;

		paramProtos[voiceKey][\velocity] = velocity;
		if( paramProtos[voiceKey][\poly] == 0,{
			if( groups[voiceKey].isPlaying, {
				indexTracker[voiceKey] = voiceLimit[voiceKey];
				if( retrigFlag == 'true',{
					if ((""++synthKeys[voiceKey]++"").contains("sample"), {
					},{
						groups[voiceKey].set(\stopGate, -1.05);
					});
				},{
					groups[voiceKey].set(\stopGate, -1.1);
				});
				if ((""++synthKeys[voiceKey]++"").contains("sample"), {
					if( retrigFlag == 'true',{
						groups[voiceKey].set(\t_trig, 1);
					},{
						groups[voiceKey].set(\t_trig, -1.05);
						Synth.new(\kildare_sample, paramProtos[voiceKey].getPairs, groups[voiceKey]);
					});
				},{
					Synth.new(synthKeys[voiceKey], paramProtos[voiceKey].getPairs, groups[voiceKey]);
				});
			});
		},{
			indexTracker[voiceKey] = (indexTracker[voiceKey] + 1) % voiceLimit[voiceKey];
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
	}*/

	setVoiceParam { arg voiceKey, paramKey, paramValue;
		var prevPoly = paramProtos[voiceKey][\poly];
		paramProtos[voiceKey][paramKey] = paramValue;

		if( paramKey == \poly, { // if the key is 'poly', then...
			(voiceLimit[voiceKey]).do({ arg voiceIndex;
				if (voiceTracker[voiceKey][voiceIndex].isPlaying, {
					voiceTracker[voiceKey][voiceIndex].free;
					('looks like '++voiceKey++', '++voiceIndex++' needed to be freed').postln;
				});
			});
			if( paramValue == 1, {
				if (groups[voiceKey].isPlaying, {
					('would want to clear mono voice for poly '++voiceKey).postln;
					('node ID: ' ++ groups[voiceKey].nodeID).postln;
					groups[voiceKey].set(\t_gate, -1.1);
					Routine {
						0.15.wait;
						('freeing this node: '++(groups[voiceKey].nodeID)).postln;
						groups[voiceKey].free;
					}.play;
				});
				(voiceLimit[voiceKey]).do({ arg voiceIndex;
					if (voiceTracker[voiceKey][voiceIndex].isPlaying, {
						voiceTracker[voiceKey][voiceIndex].free;
						('looks like '++voiceKey++', '++voiceIndex++' needed to be freed').postln;
					});
					if( emptyVoices[voiceKey] == false, {
						voiceTracker[voiceKey][voiceIndex] = Synth.new(synthKeys[voiceKey], paramProtos[voiceKey].getPairs);
						NodeWatcher.register(voiceTracker[voiceKey][voiceIndex],true);
					});
				});
			},{
				if( prevPoly != 0, {
					(voiceKey++' sending '++paramKey++ ' ' ++paramValue).postln;
					(voiceLimit[voiceKey]).do({ arg voiceIndex;
						if (voiceTracker[voiceKey][voiceIndex].isPlaying, {
							('looks like '++voiceKey++', '++voiceIndex++' needed to be freed').postln;
							voiceTracker[voiceKey][voiceIndex].set(\t_gate, -1.1);
							Routine {
								0.15.wait;
								voiceTracker[voiceKey][voiceIndex].free;
							}.play;
						});
					});
					if (groups[voiceKey].isPlaying, {
						('clearing mono voice for new mono '++voiceKey).postln;
						groups[voiceKey].free;
						groups[voiceKey] = Synth.new(synthKeys[voiceKey], paramProtos[voiceKey].getPairs);
						NodeWatcher.register(groups[voiceKey],true);
					},{
						if( emptyVoices[voiceKey] == false, {
							('making mono voice '++voiceKey).postln;
							groups[voiceKey] = Synth.new(synthKeys[voiceKey], paramProtos[voiceKey].getPairs);
							NodeWatcher.register(groups[voiceKey],true);
						});
					});
					indexTracker[voiceKey] = voiceLimit[voiceKey];
				});
			});
		},{ // if the key is not 'poly', then...
			if( paramProtos[voiceKey][\poly] == 0,
				{ // if mono:
					if( groups[voiceKey].isPlaying,
						{groups[voiceKey].set(paramKey, paramValue);}
					);
					8.do({ arg i;
						polyParams[voiceKey][i][paramKey] = paramValue; // write to poly voice storage
					});
				},
				{ // if poly:
					if ( (paramKey.asString).contains("carHz"), {
						if( voiceTracker[voiceKey][indexTracker[voiceKey]].isPlaying,
							{voiceTracker[voiceKey][indexTracker[voiceKey]].set(paramKey, paramValue)}
						);
					},{
						// set parameters for every voice:
						case
						{ polyParamStyle[voiceKey] == "all voices"}{
							(voiceLimit[voiceKey]).do{ arg i;
								if( voiceTracker[voiceKey][i].isPlaying,
									{
										voiceTracker[voiceKey][i].set(paramKey, paramValue);
									}
								);
							};
							8.do({ arg i; // write to all poly voice storage
								polyParams[voiceKey][i][paramKey] = paramValue;
							});
						}
						{ polyParamStyle[voiceKey] == "current voice"}{
							if( voiceTracker[voiceKey][indexTracker[voiceKey]].isPlaying,
								{
									voiceTracker[voiceKey][indexTracker[voiceKey]].set(paramKey, paramValue);
									polyParams[voiceKey][indexTracker[voiceKey]][paramKey] = paramValue;
								}
							);
						}
						{ polyParamStyle[voiceKey] == "next voice"}{
							if( voiceTracker[voiceKey][(indexTracker[voiceKey] + 1) % voiceLimit[voiceKey]].isPlaying,
								{
									voiceTracker[voiceKey][(indexTracker[voiceKey] + 1) % voiceLimit[voiceKey]].set(paramKey, paramValue);
									polyParams[voiceKey][(indexTracker[voiceKey] + 1) % voiceLimit[voiceKey]][paramKey] = paramValue;

								}
							);
						};
					});
				}
			);
		});
	}

	//not used yet:
	setPolyVoiceParam{ arg voiceKey, allocVoice, paramKey, paramValue;
		if ( (paramKey.asString).contains("carHz"), {
			if( voiceTracker[voiceKey][allocVoice].isPlaying,
				{voiceTracker[voiceKey][allocVoice].set(paramKey, paramValue)}
			);
		},{
			if( voiceTracker[voiceKey][allocVoice].isPlaying,
				{
					voiceTracker[voiceKey][allocVoice].set(paramKey, paramValue);
				}
			);
			polyParams[voiceKey][allocVoice][paramKey] = paramValue;
		});
	}

	savePolyParams {
		arg pathname;
		polyParams.writeArchive(pathname);
	}

	loadPolyParams {
		arg pathname;
		polyParams = Object.readArchive(pathname);
		polyParams.pairsDo({arg voiceKey, voiceID;
			voiceID.pairsDo({arg key,val;
				if( voiceTracker[voiceKey][key].isPlaying,
					{
						val.pairsDo({ arg name, value;
							voiceTracker[voiceKey][key].set(name, value);
						});
					}
				);
			});
		});
	}

	setDelayParam { arg paramKey, paramValue;
		delayParams[paramKey] = paramValue;
		outputSynths[\delay].set(paramKey, paramValue);
	}

	setFeedbackParam { arg targetKey, paramKey, paramValue;
		feedbackSynths[(targetKey).asSymbol].set(paramKey, paramValue);
	}

	setMainParam { arg paramKey, paramValue;
		mainOutParams[paramKey] = paramValue;
		outputSynths[\main].set(paramKey, paramValue);
	}

	allNotesOff {
		// topGroup.set(\stopGate, 0);
	}

	freeVoice { arg voiceKey;
		if (groups[voiceKey].isPlaying, {
			('clearing '++voiceKey).postln;
			groups[voiceKey].set(\t_gate, -1.1);
			Routine {
				0.15.wait;
				groups[voiceKey].free;
			}.play;
		});
		(voiceLimit[voiceKey]).do({ arg voiceIndex;
			if (voiceTracker[voiceKey][voiceIndex].isPlaying, {
				voiceTracker[voiceKey][voiceIndex].free;
			});
		});
		emptyVoices[voiceKey] = true;
	}

	initVoice { arg voice, model;
		emptyVoices[voice] = false;
		this.setModel(voice, model, 'true');
	}

	setVoiceLimit { arg voice, limit;
		if( paramProtos[voice][\poly] == 1,{
			(voiceLimit[voice]).do({ arg voiceIndex;
				if (voiceTracker[voice][voiceIndex].isPlaying, {
					voiceTracker[voice][voiceIndex].free;
					('looks like '++voice++', '++voiceIndex++' needed to be freed').postln;
				});
			});
		});
		voiceLimit[voice] = limit;
		if( paramProtos[voice][\poly] == 1,{
			(voiceLimit[voice]).do({ arg voiceIndex;
				if( emptyVoices[voice] == false, {
					// voiceTracker[voice][voiceIndex] = Synth.new(synthKeys[voice], paramProtos[voice].getPairs);
					voiceTracker[voice][voiceIndex] = Synth.new(synthKeys[voice], polyParams[voice][voiceIndex].getPairs);
					NodeWatcher.register(voiceTracker[voice][voiceIndex],true);
				});
			});
		});
	}

	setPolyParamStyle { arg voice, style;
		polyParamStyle[voice] = style;
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
		groups[voice].set(\t_gate, -1);
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
			groups[voice].set(\style, 1);
			groups[voice].set(\bufnum, sampleInfo[voice][\pointers][samplenum]);
			groups[voice].set(\channels, sampleInfo[voice][\samples][samplenum].numChannels);
			('channel count: '++paramProtos[voice][\channels]).postln;
			('group: ' ++ groups[voice]).postln;
		});
	}

	loadFileIntoContainer { arg voice, index, path;
		sampleInfo[voice][\samples][index] = Buffer.read(Server.default, path, action:{
			arg bufnum;
			sampleInfo[voice][\pointers][index] = bufnum;
			('pointers info: ' ++ sampleInfo[voice][\pointers][index]).postln;
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
		groups[voice].set(\t_gate, -1.1);
	}

	setModel { arg voice, model, reseed;
		var compileFlag = false;
		if (emptyVoices[voice] == false, {
			if (synthKeys[voice] != model,
				{
					compileFlag = true;
					if( paramProtos[voice][\poly] == 0,
						{
							if( groups[voice].isPlaying, {
								groups[voice].free;
								'freeing mono'.postln;
							});
						},
						{
							(voiceLimit[voice]).do({ arg voiceIndex;
								if( voiceTracker[voice][voiceIndex].isPlaying, {
									voiceTracker[voice][voiceIndex].free;
									('freeing poly '++voiceTracker[voice][voiceIndex].nodeID).postln;
								});
							});
					});
					synthKeys[voice] = model;
					paramProtos[voice] = Dictionary.newFrom(generalizedParams[model]);
					8.do({ arg i;
						('setting poly params ' ++ voice ++ ' ' ++i).postln;
						polyParams[voice][i] = Dictionary.newFrom(generalizedParams[model]);
					});
				},
				{
					if (reseed == 'true',
						{compileFlag = true});
				}
			);
			if (compileFlag, {
				('building synth ' ++ voice ++ ' ' ++ model).postln;
				if( paramProtos[voice][\poly] == 1,
					{
						(voiceLimit[voice]).do({ arg voiceIndex;
							voiceTracker[voice][voiceIndex] = Synth.new(synthKeys[voice], paramProtos[voice].getPairs);
							NodeWatcher.register(voiceTracker[voice][voiceIndex],true);
							('poly: '++voiceTracker[voice][voiceIndex].isPlaying).postln;
						});
					},
					{
						groups[voice] = Synth.new(model, paramProtos[voice].getPairs);
						NodeWatcher.register(groups[voice],true);
					}
				);
			});
		});
	}

	free {
		groups.do({arg voice;
			if( voice.isPlaying, {
				('freeing mono voice '++voice).postln;
				voice.free;
			});
		});
		// topGroup.free;
		feedbackSynths.do({arg bus;
			bus.free;
		});
		~feedback.free;
		~mixer.free;
		~processing.free;
		~main.free;
		synthDefs.do({arg def;
			def.free;
		});
		voiceTracker.do({arg voice;
			(voice).do({ arg voiceIndex;
				voiceIndex.postln;
				if (voiceIndex.isPlaying, {
					('freeing poly voice '++voiceIndex).postln;
					voiceIndex.free;
				});
			});
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