Engine_drumexample : CroneEngine {
	var pg;
	var synthArray;
	var hh_params;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		pg = ParGroup.tail(context.xg);
		synthArray = Array.newClear(10);

		SynthDef("hh", {
			arg out, hh_amp = 1,
			hh_carHz = 100, hh_carAtk = 0, hh_carRel = 0.03,
			hh_tremDepth = 1, hh_tremHz = 1000,
			hh_modAmp = 127, hh_modHz = 100, hh_modAtk = 0, hh_modRel = 2,
			hh_feedAmp = 10,
			hh_AMD = 0, hh_AMF = 303,
			hh_EQF = 600, hh_EQG = 0,
			hh_brate = 24000, hh_bcnt = 24,
			hh_LPfreq = 24000, hh_HPfreq = 0, hh_filterQ = 1,
			hh_pan = 0,
			hh_SPTCH = 1, hh_SCHNK = 1,
			kill_gate = 1;
			var hh_car, hh_mod, hh_carEnv, hh_modEnv, hh_carRamp, tremolo, tremod,
			hh_ampMod, slewLP, slewHP;
			hh_modEnv = EnvGen.kr(Env.perc(hh_modAtk, hh_modRel));
			hh_carRamp = EnvGen.kr(Env([1000, 0.000001], [hh_tremHz], curve: \exp));
			hh_carEnv = EnvGen.kr(Env.perc(hh_carAtk, hh_carRel), gate: kill_gate, doneAction:2);
			hh_ampMod = SinOsc.ar(freq:hh_AMF,mul:hh_AMD,add:1);
			hh_mod = SinOsc.ar(hh_modHz, mul:hh_modAmp) * hh_modEnv;
			hh_car = SinOscFB.ar(hh_carHz + hh_mod, hh_feedAmp) * hh_carEnv * hh_amp;
			hh_car = HPF.ar(hh_car,1100,1);
			hh_car = hh_car*hh_ampMod;
			tremolo = SinOsc.ar(hh_tremHz, 0, hh_tremDepth);
			tremod = (1.0 - hh_tremDepth) + tremolo;
			hh_car = hh_car*tremod;
			hh_car = Squiz.ar(in:hh_car, pitchratio:hh_SPTCH, zcperchunk:hh_SCHNK, mul:1);
			hh_car = BPeakEQ.ar(in:hh_car,freq:hh_EQF,rq:1,db:hh_EQG*15,mul:1);
			slewLP = Lag2.kr(hh_LPfreq,0.01);
			slewHP = Lag2.kr(hh_HPfreq,0.01);
			hh_car = BLowPass.ar(in:hh_car,freq:slewLP, rq: hh_filterQ, mul:1);
			hh_car = RHPF.ar(in:hh_car,freq:slewHP, rq: hh_filterQ, mul:1);
			hh_car = Pan2.ar(hh_car,hh_pan);
			Out.ar(out, hh_car);
		}).add;

		context.server.sync;
		synthArray = Array.fill(10,{Synth("hh",
			[ \out,0,
			\hh_amp,0
			],target:context.xg)});
		context.server.sync;

		hh_params = Dictionary.newFrom([
			\hh_amp,1,
			\hh_carHz,100,
			\hh_carAtk,0,
			\hh_carRel,0.03,
			\hh_tremDepth,1,
			\hh_tremHz,1000,
			\hh_modAmp,127,
			\hh_modHz,100,
			\hh_modAtk,0,
			\hh_modRel,2,
			\hh_feedAmp,10,
			\hh_AMD,0,
			\hh_AMF,100,
			\hh_LPfreq,24000,
			\hh_HPfreq,0,
			\hh_filterQ,1,
			\hh_pan,0,
			\hh_brate,24000,
			\hh_bcnt,24,
			\hh_SPTCH,1,
			\hh_SCHNK,1;
		]);

		hh_params.keysDo({ arg key;
			this.addCommand(key, "if", { arg msg;
				hh_params[key] = msg[2];
				if (synthArray[msg[1]-1].isRunning,{
					("setting a thing "++(hh_params[key])).postln;
					synthArray[msg[1]-1].set(hh_params[key],msg[2]);
				});
			});
		});

		this.addCommand("trig_hh", "i", { arg msg;
			if (synthArray[msg[1]-1].isRunning,{
				// ("killing previous voice "++(synthArray[msg[1]-1].nodeID)).postln;
				synthArray[msg[1]-1].set(\kill_gate,-1.05);
            });
			synthArray[msg[1]-1]=Synth.new("hh",hh_params.getPairs);
			NodeWatcher.register(synthArray[msg[1]-1]);
			("triggering a thing "++(synthArray[msg[1]-1].nodeID)).postln;
		});

			}
	free {
		(0..9).do({arg i; synthArray[i].free});
	}
}