Engine_drumexample : CroneEngine {
	var pg;
	var synthArray;
	var hh_params;
	var	busDepot;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		pg = ParGroup.tail(context.xg);
		synthArray = Array.newClear(10);
		busDepot = Dictionary.new();

		SynthDef("hh", {
			arg out, kill_gate = 1;
			var hh_amp_bus, hh_carHz_bus, hh_carAtk_bus, hh_carRel_bus,
			hh_tremDepth_bus, hh_tremHz_bus,
			hh_modAmp_bus, hh_modHz_bus, hh_modAtk_bus, hh_modRel_bus,
			hh_feedAmp_bus,
			hh_AMD_bus, hh_AMF_bus,
			hh_EQF_bus, hh_EQG_bus,
			hh_brate_bus, hh_bcnt_bus,
			hh_LPfreq_bus, hh_HPfreq_bus, hh_filterQ_bus,
			hh_pan_bus,
			hh_SPTCH_bus, hh_SCHNK_bus;

			var hh_amp,	hh_carHz, hh_carAtk, hh_carRel,
			hh_tremDepth, hh_tremHz,
			hh_modAmp, hh_modHz, hh_modAtk, hh_modRel,
			hh_feedAmp,
			hh_AMD, hh_AMF,
			hh_EQF, hh_EQG,
			hh_brate, hh_bcnt,
			hh_LPfreq, hh_HPfreq, hh_filterQ,
			hh_pan,
			hh_SPTCH, hh_SCHNK;

			var hh_car, hh_mod, hh_carEnv, hh_modEnv, hh_carRamp, tremolo, tremod,
			hh_ampMod, slewLP, slewHP;

			hh_amp=In.kr(hh_amp_bus,1);
			hh_carHz=In.kr(hh_carHz_bus,1);
			hh_carAtk=In.kr(hh_carAtk_bus,1);
			hh_carRel=In.kr(hh_carRel_bus,1);
			hh_tremDepth=In.kr(hh_tremDepth_bus,1);
			hh_tremHz=In.kr(hh_tremHz_bus,1);
			hh_modAmp=In.kr(hh_modAmp_bus,1);
			hh_modHz=In.kr(hh_modHz_bus,1);
			hh_modAtk=In.kr(hh_modAtk_bus,1);
			hh_modRel=In.kr(hh_modRel_bus,1);
			hh_feedAmp=In.kr(hh_feedAmp_bus,1);
			hh_AMD=In.kr(hh_AMD_bus,1);
			hh_AMF=In.kr(hh_AMF_bus,1);
			hh_EQF=In.kr(hh_EQF_bus,1);
			hh_EQG=In.kr(hh_EQG_bus,1);
			hh_brate=In.kr(hh_brate_bus,1);
			hh_bcnt=In.kr(hh_bcnt_bus,1);
			hh_LPfreq=In.kr(hh_LPfreq_bus,1);
			hh_HPfreq=In.kr(hh_HPfreq_bus,1);
			hh_filterQ=In.kr(hh_filterQ_bus,1);
			hh_pan=In.kr(hh_pan_bus,1);
			hh_SPTCH=In.kr(hh_SPTCH_bus,1);
			hh_SCHNK=In.kr(hh_SCHNK_bus,1);

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

		hh_params.keysValuesDo({ arg key,value;
			busDepot.put(key,Bus.control(context.server));
			busDepot.at(key++1).set(value); // TODO: instead of 1, make buses for all possible indices
			this.addCommand(key, "if", { arg msg;
				busDepot.at(key++msg[1]).set(msg[2]);
			});
		});

		this.addCommand("trig_hh", "i", { arg msg;
			if (synthArray[msg[1]-1].isRunning,{
				synthArray[msg[1]-1].set(\kill_gate,-1.05);
            });
			// synthArray[msg[1]-1]=Synth.new("hh",hh_params.getPairs);
			synthArray[msg[1]-1]=Synth.new("hh",
				\hh_amp_bus,busDepot.at(\hh_amp++msg[1]),
				\hh_carHz_bus,busDepot.at(\hh_carHz++msg[1]),
				\hh_carAtk,busDepot.at(\hh_carAtk++msg[1]),
				\hh_carRel,busDepot.at(\hh_carRel++msg[1]),
				\hh_tremDepth,busDepot.at(\hh_tremDepth++msg[1]),
				\hh_tremHz,busDepot.at(\hh_tremHz++msg[1]),
				\hh_modAmp,busDepot.at(\hh_modAmp++msg[1]),
				\hh_modHz,busDepot.at(\hh_modHz++msg[1]),
				\hh_modAtk,busDepot.at(\hh_modAtk++msg[1]),
				\hh_modRel,busDepot.at(\hh_modRel++msg[1]),
				\hh_feedAmp,busDepot.at(\hh_feedAmp++msg[1]),
				\hh_AMD,busDepot.at(\hh_AMD++msg[1]),
				\hh_AMF,busDepot.at(\hh_AMF++msg[1]),
				\hh_LPfreq,busDepot.at(\hh_LPfreq++msg[1]),
				\hh_HPfreq,busDepot.at(\hh_HPfreq++msg[1]),
				\hh_filterQ,busDepot.at(\hh_filterQ++msg[1]),
				\hh_pan,busDepot.at(\hh_pan++msg[1]),
				\hh_brate,busDepot.at(\hh_brate++msg[1]),
				\hh_bcnt,busDepot.at(\hh_bcnt++msg[1]),
				\hh_SPTCH,busDepot.at(\hh_SPTCH++msg[1]),
				\hh_SCHNK,busDepot.at(\hh_SCHNK++msg[1]);
			);

			NodeWatcher.register(synthArray[msg[1]-1]);
			("triggering a thing "++(synthArray[msg[1]-1].nodeID)).postln;
		});

			}
	free {
		(0..9).do({arg i; synthArray[i].free});
		busDepot.keysValuesDo({ arg key,value; value.free});
	}
}