KildareSample {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	init {
		SynthDef(\kildare_sample, {
			arg bufnum, out = 0, amp = 0, t_trig = 1,
			sampleStart = 0, sampleEnd = 1, loop = 0,
			pan = 0,
			sampleEnv = 0, sampleAtk = 0.01, sampleRel = 0.05,
			delayAuxL, delayAuxR, delaySend = 0,
			delayEnv, delayAtk, delayRel,
			reverbAux, reverbSend = 0,
			// baseRate = 1, rateOffset = 0, pitchControl = 0,
			rate = 1, stopGate = 1,
			amDepth, amHz,
			eqHz, eqAmp,
			bitRate, bitCount,
			lpHz, hpHz, filterQ,
			velocity = 127,
			squishPitch, squishChunk;

			var snd, snd2, pos, pos2, frames, duration, loop_env, arEnv, ampMod, delEnv, mainSend;
			var startA, endA, startB, endB, crossfade, aOrB;
			var totalOffset;

			eqHz = eqHz.lag3(0.1);
			lpHz = lpHz.lag3(0.1);
			hpHz = hpHz.lag3(0.1);
			delaySend = delaySend.lag3(0.1);
			reverbSend = reverbSend.lag3(0.1);

			filterQ = LinLin.kr(filterQ,0,100,2.0,0.001);
			eqAmp = LinLin.kr(eqAmp,-2.0,2.0,-10.0,10.0);
			amDepth = LinLin.kr(amDepth,0,1.0,0.0,2.0);

			// latch to change trigger between the two
			aOrB = ToggleFF.kr(t_trig);
			startA = Latch.kr(sampleStart,aOrB);
			endA = Latch.kr(sampleEnd,aOrB);
			startB = Latch.kr(sampleStart,1-aOrB);
			endB = Latch.kr(sampleEnd,1-aOrB);
			crossfade = Lag.ar(K2A.ar(aOrB),0.05);

			// totalOffset = rateOffset;
			// totalOffset = (0.5**((totalOffset*-1)/12)) * rate;
			// totalOffset = totalOffset + (totalOffset * (pitchControl/100));

			rate = rate*BufRateScale.kr(bufnum);
			frames = BufFrames.kr(bufnum);

			duration = Select.kr(loop > 0, [frames*(sampleEnd-sampleStart)/rate.abs/Server.default.sampleRate, inf]);

			loop_env = EnvGen.ar(
				Env.new(
					levels: [0,1,1,0],
					times: [0,duration-0.08,0.08],
				),
				gate:t_trig,
				doneAction: 2
			);

			arEnv = Select.kr(
				sampleEnv > 0, [
					1,
					EnvGen.kr(Env.linen(attackTime: sampleAtk, sustainTime: 0.08, releaseTime: sampleRel, curve: 'sin'),gate: stopGate, doneAction: sampleEnv * 2)
				]
			);

			pos=Phasor.ar(
				trig:aOrB,
				rate:rate,
				start:(((rate>0)*startA)+((rate<0)*endA))*frames,
				end:(((rate>0)*endA)+((rate<0)*startA))*frames,
				resetPos:(((rate>0)*startA)+((rate<0)*endA))*frames,
			);
			snd=BufRd.ar(
				numChannels:2,
				bufnum:bufnum,
				phase:pos,
				interpolation:4,
			);

			// add a second reader
			pos2=Phasor.ar(
				trig:(1-aOrB),
				rate:rate,
				start:(((rate>0)*startB)+((rate<0)*endB))*frames,
				end:(((rate>0)*endB)+((rate<0)*startB))*frames,
				resetPos:(((rate>0)*startB)+((rate<0)*endB))*frames,
			);
			snd2=BufRd.ar(
				numChannels:2,
				bufnum:bufnum,
				phase:pos2,
				interpolation:4,
			);

			ampMod = SinOsc.ar(freq:amHz,mul:amDepth,add:1);

			mainSend = ( ((((crossfade*snd)+((1-crossfade)*snd2)) * loop_env)*ampMod) * arEnv );

			mainSend = Squiz.ar(in:mainSend, pitchratio:squishPitch, zcperchunk:squishChunk, mul:1);
			mainSend = Decimator.ar(mainSend,bitRate,bitCount,1.0);
			mainSend = BPeakEQ.ar(in:mainSend,freq:eqHz,rq:1,db:eqAmp,mul:1);
			mainSend = RLPF.ar(in:mainSend,freq:Clip.kr(lpHz, 20, 20000), rq: filterQ, mul:1);
			mainSend = RHPF.ar(in:mainSend,freq:hpHz, rq: filterQ, mul:1);

			mainSend = Balance2.ar(mainSend[0],mainSend[1],pan);
			mainSend = mainSend * (amp * LinLin.kr(velocity,0,127,0.0,1.0));

			delEnv = Select.kr(delayEnv > 0, [delaySend, (delaySend * EnvGen.kr(Env.perc(delayAtk, delayRel, 1),gate: stopGate))]);

			Out.ar(out, mainSend);
			Out.ar(delayAuxL, (mainSend * delEnv));
			Out.ar(delayAuxR, (mainSend * delEnv));
			Out.ar(reverbAux, (mainSend * reverbSend));


		}).send;
	}
}