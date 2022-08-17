KildareSample {

	*new {
		arg srv;
		^super.new.init(srv);
	}

	init {
		SynthDef(\kildare_sample, {
			arg bufnum, out=0, amp=0, t_trig=1,
			sampleStart=0,sampleEnd=1,loop=0, loops = inf,
			pan=0,lpf=20,hpf=18000, sampleAtk = 0.01, sampleRel = 0.05,
			delayAuxL, delayAuxR, delaySend = 1,
			reverbAux, reverbSend = 1,
			rate=1, stopGate = 1;
			var snd,snd2,pos,pos2,frames,duration,loop_env, sampleEnv, mainSend;
			var startA,endA,startB,endB,crossfade,aOrB;

			// latch to change trigger between the two
			aOrB=ToggleFF.kr(t_trig);
			startA=Latch.kr(sampleStart,aOrB);
			endA=Latch.kr(sampleEnd,aOrB);
			startB=Latch.kr(sampleStart,1-aOrB);
			endB=Latch.kr(sampleEnd,1-aOrB);
			crossfade=Lag.ar(K2A.ar(aOrB),0.05);


			rate = rate*BufRateScale.kr(bufnum);
			frames = BufFrames.kr(bufnum);

			duration = Select.kr(loop > 0, [frames*(sampleEnd-sampleStart)/rate.abs/Server.default.sampleRate, inf]);

			loop_env=EnvGen.ar(
				Env.new(
					levels: [0,1,1,0],
					times: [0,duration-0.05,0.05],
				),
				gate:t_trig,
				doneAction: 2
			);

			sampleEnv = EnvGen.kr(Env.linen(attackTime: sampleAtk, sustainTime: 0.05, releaseTime: sampleRel, curve: 'sin'),gate: stopGate, doneAction: 2);

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

			mainSend = ((((crossfade*snd)+((1-crossfade)*snd2)) * loop_env) * sampleEnv);
			Out.ar(out,mainSend);
		}).send;
	}
}