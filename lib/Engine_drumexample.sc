Engine_drumexample : CroneEngine {
	var synthArray;
	var bd_params;
	var bd_busDepot;
	var sd_params;
	var	sd_busDepot;
	var xt_params;
	var	xt_busDepot;
	var cp_params;
	var	cp_busDepot;
	var rs_params;
	var	rs_busDepot;
	var cb_params;
	var	cb_busDepot;
	var hh_params;
	var	hh_busDepot;
	var cy_params;
	var	cy_busDepot;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		synthArray = Array.newClear(10);
		bd_busDepot = Dictionary.new();
		sd_busDepot = Dictionary.new();
		xt_busDepot = Dictionary.new();
		cp_busDepot = Dictionary.new();
		rs_busDepot = Dictionary.new();
		cb_busDepot = Dictionary.new();
		hh_busDepot = Dictionary.new();
		cy_busDepot = Dictionary.new();

		SynthDef("bd", {
			arg out = 0, kill_gate = 1,
			bd_amp_bus = 0, bd_carHz_bus = 0, bd_carAtk_bus = 0, bd_carRel_bus = 0,
			bd_modHz_bus = 0, bd_modAmp_bus = 0, bd_modAtk_bus = 0, bd_modRel_bus = 0, bd_feedAmp_bus = 0,
			bd_pan_bus = 0, bd_rampDepth_bus = 0, bd_rampDec_bus = 0,
			bd_SPTCH_bus = 0, bd_SCHNK_bus = 0,
			bd_AMD_bus = 0, bd_AMF_bus = 0,
			bd_EQF_bus = 0, bd_EQG_bus = 0, bd_brate_bus = 0, bd_bcnt_bus = 0,
			bd_click_bus = 0, bd_LPfreq_bus = 0, bd_HPfreq_bus = 0, bd_filterQ_bus = 0;

			var bd_amp, bd_carHz, bd_carAtk, bd_carRel,
			bd_modHz, bd_modAmp, bd_modAtk, bd_modRel, bd_feedAmp,
			bd_pan, bd_rampDepth, bd_rampDec,
			bd_SPTCH, bd_SCHNK,
			bd_AMD, bd_AMF,
			bd_EQF, bd_EQG, bd_brate, bd_bcnt,
			bd_click, bd_LPfreq, bd_HPfreq, bd_filterQ;

			var bd_car, bd_mod, bd_carEnv, bd_modEnv, bd_carRamp,
			bd_feedMod, bd_feedCar, bd_ampMod, bd_EQ, bd_clicksound,
			mod_1,slewLP, slewHP;

			bd_amp=In.kr(bd_amp_bus,1);
			bd_carHz=In.kr(bd_carHz_bus,1);
			bd_carAtk=In.kr(bd_carAtk_bus,1);
			bd_carRel=In.kr(bd_carRel_bus,1);
			bd_modHz=In.kr(bd_modHz_bus,1);
			bd_modAmp=In.kr(bd_modAmp_bus,1);
			bd_modAtk=In.kr(bd_modAtk_bus,1);
			bd_modRel=In.kr(bd_modRel_bus,1);
			bd_feedAmp=In.kr(bd_feedAmp_bus,1);
			bd_rampDepth=In.kr(bd_rampDepth_bus,1);
			bd_rampDec=In.kr(bd_rampDec_bus,1);
			bd_AMD=In.kr(bd_AMD_bus,1);
			bd_AMF=In.kr(bd_AMF_bus,1);
			bd_EQF=In.kr(bd_EQF_bus,1);
			bd_EQG=In.kr(bd_EQG_bus,1);
			bd_brate=In.kr(bd_brate_bus,1);
			bd_bcnt=In.kr(bd_bcnt_bus,1);
			bd_click=In.kr(bd_click_bus,1);
			bd_LPfreq=In.kr(bd_LPfreq_bus,1).lag3(1);
			bd_HPfreq=In.kr(bd_HPfreq_bus,1).lag3(1);
			bd_filterQ=In.kr(bd_filterQ_bus,1);
			bd_pan=In.kr(bd_pan_bus,1).lag2(0.1);
			bd_SPTCH=In.kr(bd_SPTCH_bus,1);
			bd_SCHNK=In.kr(bd_SCHNK_bus,1);

			bd_modEnv = EnvGen.kr(Env.perc(bd_modAtk, bd_modRel),gate: kill_gate);
			bd_carRamp = EnvGen.kr(Env([1000, 0.000001], [bd_rampDec], curve: \exp));
			bd_carEnv = EnvGen.kr(envelope: Env.perc(bd_carAtk, bd_carRel),gate: kill_gate, doneAction:2);

			mod_1 = SinOscFB.ar(
				bd_modHz+ ((bd_carRamp*3)*bd_rampDepth),
				bd_feedAmp,
				bd_modAmp*10
			)* bd_modEnv;

			bd_car = SinOsc.ar(bd_carHz + (mod_1) + (bd_carRamp*bd_rampDepth)) * bd_carEnv * bd_amp;
			bd_ampMod = SinOsc.ar(freq:bd_AMF,mul:(bd_AMD/2),add:1);
			bd_click = bd_amp/4;
			bd_clicksound = LPF.ar(Impulse.ar(0.003),16000,bd_click) * EnvGen.kr(envelope: Env.perc(bd_carAtk, 0.2),gate: kill_gate);
			bd_car = (bd_car + bd_clicksound)* bd_ampMod;

			bd_car = BPeakEQ.ar(in:bd_car,freq:bd_EQF,rq:1,db:bd_EQG,mul:1);
/*			slewLP = Lag2.kr(bd_LPfreq,0.1);
			slewHP = Lag2.kr(bd_HPfreq,0.1);*/
			bd_car = BLowPass.ar(in:bd_car,freq:bd_LPfreq, rq: bd_filterQ, mul:1);
			bd_car = RHPF.ar(in:bd_car,freq:bd_HPfreq, rq: bd_filterQ, mul:1);
			bd_car = Squiz.ar(in:bd_car, pitchratio:bd_SPTCH, zcperchunk:bd_SCHNK, mul:1);

			bd_car = Decimator.ar(Pan2.ar(bd_car,bd_pan),bd_brate,bd_bcnt,1.0);
			Out.ar(out, bd_car);
		}).add;

		SynthDef("sd", {
			arg out = 0, kill_gate = 1,
			sd_carHz_bus = 0, sd_modHz_bus = 0, sd_modAmp_bus = 0, sd_modAtk_bus = 0,
			sd_modRel_bus = 0, sd_carAtk_bus = 0, sd_carRel_bus = 0, sd_amp_bus = 0, sd_pan_bus = 0,
			sd_rampDepth_bus = 0, sd_rampDec_bus = 0, sd_feedAmp_bus = 0, sd_noiseAmp_bus = 0,
			sd_noiseAtk_bus = 0, sd_noiseRel_bus = 0, sd_brate_bus = 0, sd_bcnt_bus = 0,
			sd_EQF_bus = 0,sd_EQG_bus = 0, sd_click_bus = 0,
			sd_SPTCH_bus = 0, sd_SCHNK_bus = 0,
			sd_LPfreq_bus = 0, sd_HPfreq_bus = 0, sd_filterQ_bus = 0,
			sd_AMD_bus = 0, sd_AMF_bus = 0;

			var sd_carHz, sd_modHz, sd_modAmp, sd_modAtk,
			sd_modRel, sd_carAtk, sd_carRel, sd_amp, sd_pan,
			sd_rampDepth, sd_rampDec, sd_feedAmp, sd_noiseAmp,
			sd_noiseAtk, sd_noiseRel, sd_brate, sd_bcnt,
			sd_EQF,sd_EQG, sd_click,
			sd_SPTCH, sd_SCHNK,
			sd_LPfreq, sd_HPfreq, sd_filterQ,
			sd_AMD, sd_AMF;

			var sd_car, sd_mod, sd_carEnv, sd_modEnv, sd_carRamp, sd_feedMod, sd_feedCar,
			sd_noise, sd_noiseEnv, sd_mix,slewLP, slewHP, sd_ampMod;

			sd_amp=In.kr(sd_amp_bus,1);
			sd_carHz=In.kr(sd_carHz_bus,1);
			sd_carAtk=In.kr(sd_carAtk_bus,1);
			sd_carRel=In.kr(sd_carRel_bus,1);
			sd_modHz=In.kr(sd_modHz_bus,1);
			sd_modAmp=In.kr(sd_modAmp_bus,1);
			sd_modAtk=In.kr(sd_modAtk_bus,1);
			sd_modRel=In.kr(sd_modRel_bus,1);
			sd_noiseAmp=In.kr(sd_noiseAmp_bus,1);
			sd_noiseAtk=In.kr(sd_noiseAtk_bus,1);
			sd_noiseRel=In.kr(sd_noiseRel_bus,1);
			sd_feedAmp=In.kr(sd_feedAmp_bus,1);
			sd_rampDepth=In.kr(sd_rampDepth_bus,1);
			sd_rampDec=In.kr(sd_rampDec_bus,1);
			sd_AMD=In.kr(sd_AMD_bus,1);
			sd_AMF=In.kr(sd_AMF_bus,1);
			sd_EQF=In.kr(sd_EQF_bus,1);
			sd_EQG=In.kr(sd_EQG_bus,1);
			sd_brate=In.kr(sd_brate_bus,1);
			sd_bcnt=In.kr(sd_bcnt_bus,1);
			sd_click=In.kr(sd_click_bus,1);
			sd_LPfreq=In.kr(sd_LPfreq_bus,1);
			sd_HPfreq=In.kr(sd_HPfreq_bus,1);
			sd_filterQ=In.kr(sd_filterQ_bus,1);
			sd_pan=In.kr(sd_pan_bus,1).lag2(0.1);
			sd_SPTCH=In.kr(sd_SPTCH_bus,1);
			sd_SCHNK=In.kr(sd_SCHNK_bus,1);

			sd_modEnv = EnvGen.kr(Env.perc(sd_modAtk, sd_modRel));
			sd_carRamp = EnvGen.kr(Env([1000, 0.000001], [sd_rampDec], curve: \exp));
			sd_carEnv = EnvGen.kr(Env.perc(sd_carAtk, sd_carRel),gate: kill_gate);
			sd_feedMod = SinOsc.ar(sd_modHz, mul:sd_modAmp*100) * sd_modEnv;
			sd_feedAmp = sd_feedAmp * sd_modAmp; // 220224
			sd_feedCar = SinOsc.ar(sd_carHz + sd_feedMod + (sd_carRamp*sd_rampDepth)) * sd_carEnv * (sd_feedAmp/sd_modAmp * 127);
			sd_mod = SinOsc.ar(sd_modHz + sd_feedCar, mul:sd_modAmp*100) * sd_modEnv;
			sd_car = SinOsc.ar(sd_carHz + sd_mod + (sd_carRamp*sd_rampDepth)) * sd_carEnv * sd_amp;
			sd_noiseEnv = EnvGen.kr(Env.perc(sd_noiseAtk,sd_noiseRel),gate: kill_gate);
			sd_noise = BPF.ar(WhiteNoise.ar,8000,1.3) * (sd_noiseAmp*sd_noiseEnv);
			sd_noise = BPeakEQ.ar(in:sd_noise,freq:sd_EQF,rq:1,db:sd_EQG,mul:1);
			sd_noise = BLowPass.ar(in:sd_noise,freq:sd_LPfreq, rq: sd_filterQ, mul:1);
			sd_noise = RHPF.ar(in:sd_noise,freq:sd_HPfreq, rq: sd_filterQ, mul:1);

			sd_ampMod = SinOsc.ar(freq:sd_AMF,mul:(sd_AMD/2),add:1);

			sd_car = BPeakEQ.ar(in:sd_car,freq:sd_EQF,rq:1,db:sd_EQG,mul:1);
			slewLP = Lag2.kr(sd_LPfreq,0.1);
			slewHP = Lag2.kr(sd_HPfreq,0.1);
			sd_car = BLowPass.ar(in:sd_car,freq:slewLP, rq: sd_filterQ, mul:1);
			sd_car = RHPF.ar(in:sd_car,freq:slewHP, rq: sd_filterQ, mul:1);

			sd_car = Squiz.ar(in:sd_car, pitchratio:sd_SPTCH, zcperchunk:sd_SCHNK, mul:1);
        	sd_noise = Squiz.ar(in:sd_noise, pitchratio:sd_SPTCH, zcperchunk:sd_SCHNK*100, mul:1);

			sd_mix = Decimator.ar(sd_car,sd_brate,sd_bcnt,1.0);
			Out.ar(out, Pan2.ar(sd_mix,sd_pan));
			Out.ar(out, Pan2.ar(sd_noise,sd_pan));
			FreeSelf.kr(Done.kr(sd_carEnv) * Done.kr(sd_noiseEnv));
		}).add;

		SynthDef("xt", {

			arg out = 0, kill_gate = 1,
			xt_carHz_bus = 0, xt_modHz_bus = 0, xt_modAmp_bus = 0, xt_modAtk_bus = 0, xt_modRel_bus = 0, xt_feedAmp_bus = 0,
			xt_carAtk_bus = 0, xt_carRel_bus = 0, xt_amp_bus = 0,
			xt_click_bus = 0,
			xt_SPTCH_bus = 0, xt_SCHNK_bus = 0,
			xt_pan_bus = 0, xt_rampDepth_bus = 0, xt_rampDec_bus = 0, xt_AMD_bus = 0, xt_AMF_bus = 0,
			xt_EQF_bus = 0, xt_EQG_bus = 0, xt_brate_bus = 0, xt_bcnt_bus = 0,
			xt_LPfreq_bus = 0, xt_HPfreq_bus = 0, xt_filterQ_bus = 0;

			var xt_carHz, xt_modHz, xt_modAmp, xt_modAtk, xt_modRel, xt_feedAmp,
			xt_carAtk, xt_carRel, xt_amp,
			xt_click,
			xt_SPTCH, xt_SCHNK,
			xt_pan, xt_rampDepth, xt_rampDec, xt_AMD, xt_AMF,
			xt_EQF, xt_EQG, xt_brate, xt_bcnt,
			xt_LPfreq, xt_HPfreq, xt_filterQ;

			var xt_car, xt_mod, xt_carEnv, xt_modEnv, xt_carRamp, xt_feedMod,
			xt_feedCar, xt_ampMod, xt_EQ, xt_clicksound,
			mod_1,slewLP, slewHP;

			xt_amp=In.kr(xt_amp_bus,1);
			xt_carHz=In.kr(xt_carHz_bus,1);
			xt_carAtk=In.kr(xt_carAtk_bus,1);
			xt_carRel=In.kr(xt_carRel_bus,1);
			xt_modHz=In.kr(xt_modHz_bus,1);
			xt_modAmp=In.kr(xt_modAmp_bus,1);
			xt_modAtk=In.kr(xt_modAtk_bus,1);
			xt_modRel=In.kr(xt_modRel_bus,1);
			xt_feedAmp=In.kr(xt_feedAmp_bus,1);
			xt_rampDepth=In.kr(xt_rampDepth_bus,1);
			xt_rampDec=In.kr(xt_rampDec_bus,1);
			xt_AMD=In.kr(xt_AMD_bus,1);
			xt_AMF=In.kr(xt_AMF_bus,1);
			xt_EQF=In.kr(xt_EQF_bus,1);
			xt_EQG=In.kr(xt_EQG_bus,1);
			xt_brate=In.kr(xt_brate_bus,1);
			xt_bcnt=In.kr(xt_bcnt_bus,1);
			xt_click=In.kr(xt_click_bus,1);
			xt_LPfreq=In.kr(xt_LPfreq_bus,1);
			xt_HPfreq=In.kr(xt_HPfreq_bus,1);
			xt_filterQ=In.kr(xt_filterQ_bus,1);
			xt_pan=In.kr(xt_pan_bus,1).lag2(0.1);
			xt_SPTCH=In.kr(xt_SPTCH_bus,1);
			xt_SCHNK=In.kr(xt_SCHNK_bus,1);

			xt_modEnv = EnvGen.kr(Env.perc(xt_modAtk, xt_modRel));
			xt_carRamp = EnvGen.kr(Env([600, 0.000001], [xt_rampDec], curve: \lin));
			xt_carEnv = EnvGen.kr(Env.perc(xt_carAtk, xt_carRel), gate: kill_gate, doneAction:2);

			mod_1 = SinOscFB.ar(
				xt_modHz,
				xt_feedAmp,
				xt_modAmp*10
			)* xt_modEnv;

			xt_car = SinOsc.ar(xt_carHz + (mod_1) + (xt_carRamp*xt_rampDepth)) * xt_carEnv * xt_amp;

			xt_ampMod = SinOsc.ar(freq:xt_AMF,mul:xt_AMD,add:1);
			xt_clicksound = LPF.ar(Impulse.ar(0.003),16000,xt_click) * EnvGen.kr(envelope: Env.perc(xt_carAtk, 0.2),gate: kill_gate);
			xt_car = (xt_car + xt_clicksound) * xt_ampMod;
			xt_car = Squiz.ar(in:xt_car, pitchratio:xt_SPTCH, zcperchunk:xt_SCHNK, mul:1);

			xt_car = BPeakEQ.ar(in:xt_car,freq:xt_EQF,rq:1,db:xt_EQG,mul:1);

			slewLP = Lag2.kr(xt_LPfreq,0.1);
			slewHP = Lag2.kr(xt_HPfreq,0.1);
			xt_car = BLowPass.ar(in:xt_car,freq:slewLP, rq: xt_filterQ, mul:1);
			xt_car = RHPF.ar(in:xt_car,freq:slewHP, rq: xt_filterQ, mul:1);

			xt_car = Decimator.ar(Pan2.ar(xt_car,xt_pan),xt_brate,xt_bcnt,1.0);
			Out.ar(out, xt_car);
		}).add;

		SynthDef("cp", {
			arg out = 0, kill_gate = 1,
			cp_carHz_bus = 0,
			cp_modHz_bus = 0, cp_modAmp_bus = 0, cp_modRel_bus = 0, cp_feedAmp_bus = 0,
			cp_carRel_bus = 0, cp_amp_bus = 0, cp_click_bus = 0,
			cp_SPTCH_bus = 0, cp_SCHNK_bus = 0,
			cp_pan_bus = 0, cp_AMD_bus = 0, cp_AMF_bus = 0,
			cp_EQF_bus = 0, cp_EQG_bus = 0, cp_brate_bus = 0, cp_bcnt_bus = 0,
			cp_LPfreq_bus = 0, cp_HPfreq_bus = 0, cp_filterQ_bus = 0;

			var cp_carHz,
			cp_modHz, cp_modAmp, cp_modRel, cp_feedAmp,
			cp_carRel, cp_amp, cp_click,
			cp_SPTCH, cp_SCHNK,
			cp_pan, cp_AMD, cp_AMF,
			cp_EQF, cp_EQG, cp_brate, cp_bcnt,
			cp_LPfreq, cp_HPfreq, cp_filterQ;

			var cp_car, cp_mod, cp_carEnv, cp_modEnv, cp_carRamp, cp_feedMod, cp_feedCar, cp_ampMod, cp_EQ,
			mod_1,mod_2,
			slewLP, slewHP;

			cp_amp=In.kr(cp_amp_bus,1);
			cp_carHz=In.kr(cp_carHz_bus,1);
			cp_carRel=In.kr(cp_carRel_bus,1);
			cp_modHz=In.kr(cp_modHz_bus,1);
			cp_modAmp=In.kr(cp_modAmp_bus,1);
			cp_modRel=In.kr(cp_modRel_bus,1);
			cp_feedAmp=In.kr(cp_feedAmp_bus,1);
			cp_AMD=In.kr(cp_AMD_bus,1);
			cp_AMF=In.kr(cp_AMF_bus,1);
			cp_EQF=In.kr(cp_EQF_bus,1);
			cp_EQG=In.kr(cp_EQG_bus,1);
			cp_brate=In.kr(cp_brate_bus,1);
			cp_bcnt=In.kr(cp_bcnt_bus,1);
			cp_click=In.kr(cp_click_bus,1);
			cp_LPfreq=In.kr(cp_LPfreq_bus,1);
			cp_HPfreq=In.kr(cp_HPfreq_bus,1);
			cp_filterQ=In.kr(cp_filterQ_bus,1);
			cp_pan=In.kr(cp_pan_bus,1).lag2(0.1);
			cp_SPTCH=In.kr(cp_SPTCH_bus,1);
			cp_SCHNK=In.kr(cp_SCHNK_bus,1);

			cp_modEnv = EnvGen.ar(
				Env.new(
					[0, 1, 0, 0.9, 0, 0.7, 0, 0.5, 0],
					[0.001, 0.009, 0, 0.008, 0, 0.01, 0, cp_modRel],
					curve: \lin
				),gate: kill_gate
			);
			cp_carRamp = EnvGen.kr(Env([600, 0.000001], [0], curve: \lin));
			cp_carEnv = EnvGen.ar(
				Env.new(
					[0, 1, 0, 0.9, 0, 0.7, 0, 0.5, 0],
					[0,0,0,0,0,0,0,cp_carRel/4],
					[0, -3, 0, -3, 0, -3, 0, -3]
					// curve:\squared
				),gate: kill_gate
			);

			mod_2 = SinOscFB.ar(
				(cp_modHz*4),
				cp_feedAmp,
				0,
				cp_modAmp*1
			)* cp_modEnv;

			mod_1 = SinOscFB.ar(
				cp_modHz+mod_2,
				cp_feedAmp,
				cp_modAmp*100
			)* cp_modEnv;

			cp_car = SinOsc.ar(cp_carHz + (mod_1)) * cp_carEnv * cp_amp;
			cp_car = RHPF.ar(in:cp_car+(LPF.ar(Impulse.ar(0.003),12000,1)*cp_click),freq:cp_HPfreq,rq:cp_filterQ,mul:1);

			cp_ampMod = SinOsc.ar(freq:cp_AMF,mul:cp_AMD,add:1);
			cp_car = cp_car * cp_ampMod;

			cp_car = Squiz.ar(in:cp_car, pitchratio:cp_SPTCH, zcperchunk:cp_SCHNK, mul:1);

			cp_car = BPeakEQ.ar(in:cp_car,freq:cp_EQF,rq:1,db:cp_EQG,mul:1);

			cp_car = Decimator.ar(Pan2.ar(cp_car,cp_pan),cp_brate,cp_bcnt,1.0);

			slewLP = Lag2.kr(cp_LPfreq,0.1);
			slewHP = Lag2.kr(cp_HPfreq,0.1);
			cp_car = BLowPass.ar(in:cp_car,freq:slewLP, rq: cp_filterQ, mul:1);
			cp_car = RHPF.ar(in:cp_car,freq:slewHP, rq: cp_filterQ, mul:1);

			cp_car = cp_car.softclip;
			Out.ar(out, cp_car);
			FreeSelf.kr(Done.kr(cp_modEnv) * Done.kr(cp_carEnv));
		}).add;

		SynthDef("cb", {
			arg out, kill_gate = 1,
			cb_carHz_bus = 0,
			cb_modHz_bus = 0, cb_modAmp_bus = 0, cb_modAtk_bus = 0, cb_modRel_bus = 0, cb_feedAmp_bus = 0,
			cb_carAtk_bus = 0, cb_carRel_bus = 0, cb_amp_bus = 0, cb_click_bus = 0,
			cb_snap_bus = 0,
			cb_pan_bus = 0, cb_rampDepth_bus = 0, cb_rampDec_bus = 0, cb_AMD_bus = 0, cb_AMF_bus = 0,
			cb_EQF_bus = 0, cb_EQG_bus = 0, cb_brate_bus = 0, cb_bcnt_bus = 0,
			cb_LPfreq_bus = 0, cb_HPfreq_bus = 0, cb_filterQ_bus = 0,
			cb_SPTCH_bus = 0, cb_SCHNK_bus = 0;

			var cb_carHz,
			cb_modHz, cb_modAmp, cb_modAtk, cb_modRel, cb_feedAmp,
			cb_carAtk, cb_carRel, cb_amp, cb_click,
			cb_snap,
			cb_pan, cb_rampDepth, cb_rampDec, cb_AMD, cb_AMF,
			cb_EQF, cb_EQG, cb_brate, cb_bcnt,
			cb_LPfreq, cb_HPfreq, cb_filterQ,
			cb_SPTCH, cb_SCHNK;

			var cb_car, cb_mod, cb_carEnv, cb_modEnv, cb_carRamp, cb_feedMod, cb_feedCar, cb_ampMod, cb_EQ,
			sig,cb_1,cb_2,klank_env,other_mod1,other_mod2,
			slewLP, slewHP;

			cb_amp=In.kr(cb_amp_bus,1);
			cb_carHz=In.kr(cb_carHz_bus,1);
			cb_carAtk=In.kr(cb_carAtk_bus,1);
			cb_carRel=In.kr(cb_carRel_bus,1);
			cb_modHz=In.kr(cb_modHz_bus,1);
			cb_modAmp=In.kr(cb_modAmp_bus,1);
			cb_modAtk=In.kr(cb_modAtk_bus,1);
			cb_modRel=In.kr(cb_modRel_bus,1);
			cb_feedAmp=In.kr(cb_feedAmp_bus,1);
			cb_rampDepth=In.kr(cb_rampDepth_bus,1);
			cb_rampDec=In.kr(cb_rampDec_bus,1);
			cb_AMD=In.kr(cb_AMD_bus,1);
			cb_AMF=In.kr(cb_AMF_bus,1);
			cb_EQF=In.kr(cb_EQF_bus,1);
			cb_EQG=In.kr(cb_EQG_bus,1);
			cb_brate=In.kr(cb_brate_bus,1);
			cb_bcnt=In.kr(cb_bcnt_bus,1);
			cb_click=In.kr(cb_click_bus,1);
			cb_snap=In.kr(cb_snap_bus,1);
			cb_LPfreq=In.kr(cb_LPfreq_bus,1).lag3(1);
			cb_HPfreq=In.kr(cb_HPfreq_bus,1).lag3(1);
			cb_filterQ=In.kr(cb_filterQ_bus,1);
			cb_pan=In.kr(cb_pan_bus,1).lag2(0.1);
			cb_SPTCH=In.kr(cb_SPTCH_bus,1);
			cb_SCHNK=In.kr(cb_SCHNK_bus,1);

			cb_modEnv = EnvGen.kr(Env.perc(cb_modAtk, cb_modRel), gate:kill_gate);
			cb_carRamp = EnvGen.kr(Env([600, 0.000001], [cb_rampDec], curve: \lin));
			cb_carEnv = EnvGen.kr(Env.perc(cb_carAtk, cb_carRel),gate: kill_gate);

			cb_1 = LFPulse.ar((cb_carHz) + (cb_carRamp*cb_rampDepth)) * cb_carEnv * cb_amp;
			cb_feedAmp = cb_feedAmp.linlin(0,127,1,3);
			cb_2 = SinOscFB.ar((cb_carHz*1.5085)+ (cb_carRamp*cb_rampDepth),cb_feedAmp) * cb_carEnv * cb_amp;
			cb_ampMod = SinOsc.ar(freq:cb_AMF,mul:cb_AMD,add:1);
			cb_1 = (cb_1+(LPF.ar(Impulse.ar(0.003),16000,1)*cb_snap)) * cb_ampMod;
			cb_2 = (cb_2+(LPF.ar(Impulse.ar(0.003),16000,1)*cb_snap)) * cb_ampMod;
			cb_1 = BPeakEQ.ar(in:cb_1,freq:cb_EQF,rq:1,db:cb_EQG,mul:1);
			cb_2 = BPeakEQ.ar(in:cb_2,freq:cb_EQF,rq:1,db:cb_EQG,mul:1);
/*			slewLP = Lag2.kr(cb_LPfreq,0.1);
			slewHP = Lag2.kr(cb_HPfreq,0.1);*/
			cb_1 = BLowPass.ar(in:cb_1,freq:cb_LPfreq, rq: cb_filterQ, mul:1);
			cb_2 = BLowPass.ar(in:cb_2,freq:cb_LPfreq, rq: cb_filterQ, mul:1);
			cb_1 = RHPF.ar(in:cb_1,freq:cb_HPfreq, rq: cb_filterQ, mul:1);
			cb_2 = RHPF.ar(in:cb_2,freq:cb_HPfreq, rq: cb_filterQ, mul:1);
			cb_1 = Decimator.ar(Pan2.ar(cb_1,cb_pan),cb_brate,cb_bcnt,1.0);
			cb_2 = Decimator.ar(Pan2.ar(cb_2,cb_pan),cb_brate,cb_bcnt,1.0);
			sig = (DynKlank.ar(`
				[[cb_modHz,cb_modHz*1.5085, cb_modHz*3.017, cb_modHz*4.5255, cb_modHz*5.27975,cb_modHz*7.5425, cb_modHz*9.051,cb_modHz*10.5595,cb_modHz*18.102]
					, [0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.05],
					[cb_modRel, cb_modRel, cb_modRel, cb_modRel,cb_modRel,cb_modRel,cb_modRel,cb_modRel,cb_modRel]],
			Impulse.ar(0.003))*cb_click) * cb_modEnv * cb_modAmp.linlin(0,127,0,1);
			sig = LPF.ar(sig,2000);
			sig = (sig+(LPF.ar(Impulse.ar(0.003),16000,1)*cb_snap)) * cb_ampMod;
			sig = BPeakEQ.ar(in:sig,freq:cb_EQF,rq:1,db:cb_EQG,mul:1);

			sig = BLowPass.ar(in:sig,freq:cb_LPfreq, rq: cb_filterQ, mul:1);
			sig = RHPF.ar(in:sig,freq:cb_HPfreq, rq: cb_filterQ, mul:1);
			sig = Decimator.ar(Pan2.ar(sig,cb_pan),cb_brate,cb_bcnt,1.0);
			cb_1 = (cb_1*0.33)+(cb_2*0.33)+(sig);
			cb_1 = Squiz.ar(in:cb_1, pitchratio:cb_SPTCH, zcperchunk:cb_SCHNK, mul:1);
			Out.ar(out, cb_1);
			FreeSelf.kr(Done.kr(cb_carEnv) * Done.kr(cb_modEnv));
		}).add;

		SynthDef("hh", {
			arg out=0, kill_gate = 1,
			hh_amp_bus=0, hh_carHz_bus=0, hh_carAtk_bus=0, hh_carRel_bus=0,
			hh_tremDepth_bus=0, hh_tremHz_bus=0,
			hh_modAmp_bus=0, hh_modHz_bus=0, hh_modAtk_bus=0, hh_modRel_bus=0,
			hh_feedAmp_bus=0,
			hh_AMD_bus=0, hh_AMF_bus=0,
			hh_EQF_bus=0, hh_EQG_bus=0,
			hh_brate_bus=0, hh_bcnt_bus=0,
			hh_LPfreq_bus=0, hh_HPfreq_bus=0, hh_filterQ_bus=0,
			hh_pan_bus=0,
			hh_SPTCH_bus=0, hh_SCHNK_bus=0;

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
			hh_pan=In.kr(hh_pan_bus,1).lag2(0.1);
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

		bd_params = Dictionary.newFrom([
			\bd_amp,1,
			\bd_carHz,55,
			\bd_carAtk,0,
			\bd_carRel,0.3,
			\bd_modHz,600,
			\bd_modAmp,127,
			\bd_modAtk,0,
			\bd_modRel,0.05,
			\bd_feedAmp,10,
			\bd_pan,0,
			\bd_rampDepth,0.5,
			\bd_rampDec,0.3,
			\bd_SPTCH,1,
			\bd_SCHNK,1,
			\bd_AMD,1,
			\bd_AMF,2698.8,
			\bd_EQF,6000,
			\bd_EQG,0,
			\bd_brate,24000,
			\bd_bcnt,24,
			\bd_click,1,
			\bd_LPfreq,19000,
			\bd_HPfreq,0,
			\bd_filterQ,1,
		]);

		bd_params.keysValuesDo({ arg key,value;
			bd_busDepot.put(key++1,Bus.control(context.server));
			bd_busDepot.at(key++1).set(value);
			this.addCommand(key, "if", { arg msg;
				// NOTE: added a little debug here
				["setting",key,msg[1],msg[2],bd_busDepot.at(key++msg[1])].postln;
				bd_busDepot.at(key++msg[1]).set(msg[2]);
			});
		});

		this.addCommand("trig_bd", "i", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[msg[1]-1].notNil,{
				if (synthArray[msg[1]-1].isRunning,{
					synthArray[msg[1]-1].set(\kill_gate,-1.05);
				});
			});
			[(\bd_amp++msg[1]),bd_busDepot.at(\bd_amp++msg[1])].postln;
			// synthArray[msg[1]-1]=Synth.new("hh",bd_params.getPairs);
			synthArray[msg[1]-1]=Synth.new("bd",[
				\bd_amp_bus,bd_busDepot.at(\bd_amp++msg[1]).index.postln,
				\bd_carHz_bus,bd_busDepot.at(\bd_carHz++msg[1]),
				\bd_carAtk_bus,bd_busDepot.at(\bd_carAtk++msg[1]),
				\bd_carRel_bus,bd_busDepot.at(\bd_carRel++msg[1]),
				\bd_modAmp_bus,bd_busDepot.at(\bd_modAmp++msg[1]),
				\bd_modHz_bus,bd_busDepot.at(\bd_modHz++msg[1]),
				\bd_modAtk_bus,bd_busDepot.at(\bd_modAtk++msg[1]),
				\bd_modRel_bus,bd_busDepot.at(\bd_modRel++msg[1]),
				\bd_feedAmp_bus,bd_busDepot.at(\bd_feedAmp++msg[1]),
				\bd_rampDepth_bus,bd_busDepot.at(\bd_rampDepth++msg[1]),
				\bd_rampDec_bus,bd_busDepot.at(\bd_rampDec++msg[1]),
				\bd_click_bus,bd_busDepot.at(\bd_click++msg[1]),
				\bd_AMD_bus,bd_busDepot.at(\bd_AMD++msg[1]),
				\bd_AMF_bus,bd_busDepot.at(\bd_AMF++msg[1]),
				\bd_EQF_bus,bd_busDepot.at(\bd_EQF++msg[1]),
				\bd_EQG_bus,bd_busDepot.at(\bd_EQG++msg[1]),
				\bd_LPfreq_bus,bd_busDepot.at(\bd_LPfreq++msg[1]),
				\bd_HPfreq_bus,bd_busDepot.at(\bd_HPfreq++msg[1]),
				\bd_filterQ_bus,bd_busDepot.at(\bd_filterQ++msg[1]),
				\bd_pan_bus,bd_busDepot.at(\bd_pan++msg[1]),
				\bd_brate_bus,bd_busDepot.at(\bd_brate++msg[1]),
				\bd_bcnt_bus,bd_busDepot.at(\bd_bcnt++msg[1]),
				\bd_SPTCH_bus,bd_busDepot.at(\bd_SPTCH++msg[1]),
				\bd_SCHNK_bus,bd_busDepot.at(\bd_SCHNK++msg[1]),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[msg[1]-1]);
			("triggering a thing "++(synthArray[msg[1]-1].nodeID)).postln;
		});

		sd_params = Dictionary.newFrom([
			\sd_amp,1,
			\sd_carHz,277.2,
			\sd_carAtk,0,
			\sd_carRel,0.3,
			\sd_modHz,2770,
			\sd_modAmp,0,
			\sd_modAtk,0.2,
			\sd_modRel,1,
			\sd_noiseAmp,0.01,
			\sd_noiseAtk,0,
			\sd_noiseRel,0.1,
			\sd_feedAmp,0,
			\sd_pan,0,
			\sd_rampDepth,1,
			\sd_rampDec,0.06,
			\sd_SPTCH,1,
			\sd_SCHNK,1,
			\sd_AMD,0,
			\sd_AMF,2698.8,
			\sd_EQF,12000,
			\sd_EQG,20,
			\sd_brate,24000,
			\sd_bcnt,24,
			\sd_click,1,
			\sd_LPfreq,19000,
			\sd_HPfreq,0,
			\sd_filterQ,1,
		]);

		sd_params.keysValuesDo({ arg key,value;
			sd_busDepot.put(key++2,Bus.control(context.server));
			sd_busDepot.at(key++2).set(value);
			this.addCommand(key, "if", { arg msg;
				// NOTE: added a little debug here
				["setting",key,msg[1],msg[2],sd_busDepot.at(key++msg[1])].postln;
				sd_busDepot.at(key++msg[1]).set(msg[2]);
			});
		});

		this.addCommand("trig_sd", "i", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[msg[1]-1].notNil,{
				if (synthArray[msg[1]-1].isRunning,{
					synthArray[msg[1]-1].set(\kill_gate,-1.05);
				});
			});
			[(\sd_amp++msg[1]),sd_busDepot.at(\sd_amp++msg[1])].postln;
			// synthArray[msg[1]-1]=Synth.new("hh",sd_params.getPairs);
			synthArray[msg[1]-1]=Synth.new("sd",[
				\sd_amp_bus,sd_busDepot.at(\sd_amp++msg[1]).index.postln,
				\sd_carHz_bus,sd_busDepot.at(\sd_carHz++msg[1]),
				\sd_carAtk_bus,sd_busDepot.at(\sd_carAtk++msg[1]),
				\sd_carRel_bus,sd_busDepot.at(\sd_carRel++msg[1]),
				\sd_modAmp_bus,sd_busDepot.at(\sd_modAmp++msg[1]),
				\sd_modHz_bus,sd_busDepot.at(\sd_modHz++msg[1]),
				\sd_modAtk_bus,sd_busDepot.at(\sd_modAtk++msg[1]),
				\sd_modRel_bus,sd_busDepot.at(\sd_modRel++msg[1]),
				\sd_noiseAmp_bus,sd_busDepot.at(\sd_noiseAmp++msg[1]),
				\sd_noiseAtk_bus,sd_busDepot.at(\sd_noiseAtk++msg[1]),
				\sd_noiseRel_bus,sd_busDepot.at(\sd_noiseRel++msg[1]),
				\sd_feedAmp_bus,sd_busDepot.at(\sd_feedAmp++msg[1]),
				\sd_rampDepth_bus,sd_busDepot.at(\sd_rampDepth++msg[1]),
				\sd_rampDec_bus,sd_busDepot.at(\sd_rampDec++msg[1]),
				\sd_click_bus,sd_busDepot.at(\sd_click++msg[1]),
				\sd_AMD_bus,sd_busDepot.at(\sd_AMD++msg[1]),
				\sd_AMF_bus,sd_busDepot.at(\sd_AMF++msg[1]),
				\sd_EQF_bus,sd_busDepot.at(\sd_EQF++msg[1]),
				\sd_EQG_bus,sd_busDepot.at(\sd_EQG++msg[1]),
				\sd_LPfreq_bus,sd_busDepot.at(\sd_LPfreq++msg[1]),
				\sd_HPfreq_bus,sd_busDepot.at(\sd_HPfreq++msg[1]),
				\sd_filterQ_bus,sd_busDepot.at(\sd_filterQ++msg[1]),
				\sd_pan_bus,sd_busDepot.at(\sd_pan++msg[1]),
				\sd_brate_bus,sd_busDepot.at(\sd_brate++msg[1]),
				\sd_bcnt_bus,sd_busDepot.at(\sd_bcnt++msg[1]),
				\sd_SPTCH_bus,sd_busDepot.at(\sd_SPTCH++msg[1]),
				\sd_SCHNK_bus,sd_busDepot.at(\sd_SCHNK++msg[1]),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[msg[1]-1]);
			("triggering a thing "++(synthArray[msg[1]-1].nodeID)).postln;
		});

		xt_params = Dictionary.newFrom([
			\xt_amp,1,
			\xt_carHz,87.3,
			\xt_carAtk,0,
			\xt_carRel,0.3,
			\xt_modHz,174.6,
			\xt_modAmp,100,
			\xt_modAtk,0,
			\xt_modRel,0.2,
			\xt_feedAmp,21,
			\xt_pan,0,
			\xt_rampDepth,0.3,
			\xt_rampDec,0.13,
			\xt_SPTCH,1,
			\xt_SCHNK,1,
			\xt_AMD,0,
			\xt_AMF,2698.8,
			\xt_EQF,6000,
			\xt_EQG,0,
			\xt_brate,24000,
			\xt_bcnt,24,
			\xt_click,1,
			\xt_LPfreq,19000,
			\xt_HPfreq,0,
			\xt_filterQ,0.5,
		]);

		xt_params.keysValuesDo({ arg key,value;
			xt_busDepot.put(key++3,Bus.control(context.server));
			xt_busDepot.at(key++3).set(value);
			this.addCommand(key, "if", { arg msg;
				// NOTE: added a little debug here
				["setting",key,msg[1],msg[2],xt_busDepot.at(key++msg[1])].postln;
				xt_busDepot.at(key++msg[1]).set(msg[2]);
			});
		});

		this.addCommand("trig_xt", "i", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[msg[1]-1].notNil,{
				if (synthArray[msg[1]-1].isRunning,{
					synthArray[msg[1]-1].set(\kill_gate,-1.05);
				});
			});
			[(\xt_amp++msg[1]),xt_busDepot.at(\xt_amp++msg[1])].postln;
			// synthArray[msg[1]-1]=Synth.new("hh",xt_params.getPairs);
			synthArray[msg[1]-1]=Synth.new("xt",[
				\xt_amp_bus,xt_busDepot.at(\xt_amp++msg[1]).index.postln,
				\xt_carHz_bus,xt_busDepot.at(\xt_carHz++msg[1]),
				\xt_carAtk_bus,xt_busDepot.at(\xt_carAtk++msg[1]),
				\xt_carRel_bus,xt_busDepot.at(\xt_carRel++msg[1]),
				\xt_modAmp_bus,xt_busDepot.at(\xt_modAmp++msg[1]),
				\xt_modHz_bus,xt_busDepot.at(\xt_modHz++msg[1]),
				\xt_modAtk_bus,xt_busDepot.at(\xt_modAtk++msg[1]),
				\xt_modRel_bus,xt_busDepot.at(\xt_modRel++msg[1]),
				\xt_feedAmp_bus,xt_busDepot.at(\xt_feedAmp++msg[1]),
				\xt_rampDepth_bus,xt_busDepot.at(\xt_rampDepth++msg[1]),
				\xt_rampDec_bus,xt_busDepot.at(\xt_rampDec++msg[1]),
				\xt_click_bus,xt_busDepot.at(\xt_click++msg[1]),
				\xt_AMD_bus,xt_busDepot.at(\xt_AMD++msg[1]),
				\xt_AMF_bus,xt_busDepot.at(\xt_AMF++msg[1]),
				\xt_EQF_bus,xt_busDepot.at(\xt_EQF++msg[1]),
				\xt_EQG_bus,xt_busDepot.at(\xt_EQG++msg[1]),
				\xt_LPfreq_bus,xt_busDepot.at(\xt_LPfreq++msg[1]),
				\xt_HPfreq_bus,xt_busDepot.at(\xt_HPfreq++msg[1]),
				\xt_filterQ_bus,xt_busDepot.at(\xt_filterQ++msg[1]),
				\xt_pan_bus,xt_busDepot.at(\xt_pan++msg[1]),
				\xt_brate_bus,xt_busDepot.at(\xt_brate++msg[1]),
				\xt_bcnt_bus,xt_busDepot.at(\xt_bcnt++msg[1]),
				\xt_SPTCH_bus,xt_busDepot.at(\xt_SPTCH++msg[1]),
				\xt_SCHNK_bus,xt_busDepot.at(\xt_SCHNK++msg[1]),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[msg[1]-1]);
			("triggering a thing "++(synthArray[msg[1]-1].nodeID)).postln;
		});

		cp_params = Dictionary.newFrom([
			\cp_amp,1,
			\cp_carHz,450,
			\cp_carRel,0.5,
			\cp_modHz,300,
			\cp_modAmp,127,
			\cp_modRel,0.5,
			\cp_feedAmp,10,
			\cp_pan,0,
			\cp_SPTCH,1,
			\cp_SCHNK,1,
			\cp_AMD,0,
			\cp_AMF,127,
			\cp_EQF,600,
			\cp_EQG,0,
			\cp_brate,24000,
			\cp_bcnt,24,
			\cp_click,1,
			\cp_LPfreq,24000,
			\cp_HPfreq,0,
			\cp_filterQ,1,
		]);

		cp_params.keysValuesDo({ arg key,value;
			cp_busDepot.put(key++4,Bus.control(context.server));
			cp_busDepot.at(key++4).set(value);
			this.addCommand(key, "if", { arg msg;
				// NOTE: added a little debug here
				["setting",key,msg[1],msg[2],cp_busDepot.at(key++msg[1])].postln;
				cp_busDepot.at(key++msg[1]).set(msg[2]);
			});
		});

		this.addCommand("trig_cp", "i", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[msg[1]-1].notNil,{
				if (synthArray[msg[1]-1].isRunning,{
					synthArray[msg[1]-1].set(\kill_gate,-1.05);
				});
			});
			[(\cp_amp++msg[1]),cp_busDepot.at(\cp_amp++msg[1])].postln;
			// synthArray[msg[1]-1]=Synth.new("hh",cp_params.getPairs);
			synthArray[msg[1]-1]=Synth.new("cp",[
				\cp_amp_bus,cp_busDepot.at(\cp_amp++msg[1]).index.postln,
				\cp_carHz_bus,cp_busDepot.at(\cp_carHz++msg[1]),
				\cp_carRel_bus,cp_busDepot.at(\cp_carRel++msg[1]),
				\cp_modAmp_bus,cp_busDepot.at(\cp_modAmp++msg[1]),
				\cp_modHz_bus,cp_busDepot.at(\cp_modHz++msg[1]),
				\cp_modRel_bus,cp_busDepot.at(\cp_modRel++msg[1]),
				\cp_feedAmp_bus,cp_busDepot.at(\cp_feedAmp++msg[1]),
				\cp_click_bus,cp_busDepot.at(\cp_click++msg[1]),
				\cp_AMD_bus,cp_busDepot.at(\cp_AMD++msg[1]),
				\cp_AMF_bus,cp_busDepot.at(\cp_AMF++msg[1]),
				\cp_EQF_bus,cp_busDepot.at(\cp_EQF++msg[1]),
				\cp_EQG_bus,cp_busDepot.at(\cp_EQG++msg[1]),
				\cp_LPfreq_bus,cp_busDepot.at(\cp_LPfreq++msg[1]),
				\cp_HPfreq_bus,cp_busDepot.at(\cp_HPfreq++msg[1]),
				\cp_filterQ_bus,cp_busDepot.at(\cp_filterQ++msg[1]),
				\cp_pan_bus,cp_busDepot.at(\cp_pan++msg[1]),
				\cp_brate_bus,cp_busDepot.at(\cp_brate++msg[1]),
				\cp_bcnt_bus,cp_busDepot.at(\cp_bcnt++msg[1]),
				\cp_SPTCH_bus,cp_busDepot.at(\cp_SPTCH++msg[1]),
				\cp_SCHNK_bus,cp_busDepot.at(\cp_SCHNK++msg[1]),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[msg[1]-1]);
			("triggering a thing "++(synthArray[msg[1]-1].nodeID)).postln;
		});

		cb_params = Dictionary.newFrom([
			\cb_amp,1,
			\cb_carHz,404,
			\cb_carAtk,0,
			\cb_carRel,2,
			\cb_modHz,300,
			\cb_modAmp,0,
			\cb_modAtk,0,
			\cb_modRel,2,
			\cb_feedAmp,0,
			\cb_pan,0,
			\cb_click,1,
			\cb_snap,0,
			\cb_rampDepth,0,
			\cb_rampDec,4,
			\cb_SPTCH,1,
			\cb_SCHNK,1,
			\cb_AMD,0,
			\cb_AMF,303,
			\cb_EQF,600,
			\cb_EQG,0,
			\cb_brate,24000,
			\cb_bcnt,24,
			\cb_LPfreq,24000,
			\cb_HPfreq,0,
			\cb_filterQ,0.5,
		]);

		cb_params.keysValuesDo({ arg key,value;
			cb_busDepot.put(key++5,Bus.control(context.server));
			cb_busDepot.at(key++5).set(value);
			this.addCommand(key, "if", { arg msg;
				// NOTE: added a little debug here
				["setting",key,msg[1],msg[2],cb_busDepot.at(key++msg[1])].postln;
				cb_busDepot.at(key++msg[1]).set(msg[2]);
			});
		});

		this.addCommand("trig_cb", "i", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[msg[1]-1].notNil,{
				if (synthArray[msg[1]-1].isRunning,{
					synthArray[msg[1]-1].set(\kill_gate,-1.05);
				});
			});
			[(\cb_amp++msg[1]),cb_busDepot.at(\cb_amp++msg[1])].postln;
			// synthArray[msg[1]-1]=Synth.new("hh",cb_params.getPairs);
			synthArray[msg[1]-1]=Synth.new("cb",[
				\cb_amp_bus,cb_busDepot.at(\cb_amp++msg[1]).index.postln,
				\cb_carHz_bus,cb_busDepot.at(\cb_carHz++msg[1]),
				\cb_carAtk_bus,cb_busDepot.at(\cb_carAtk++msg[1]),
				\cb_carRel_bus,cb_busDepot.at(\cb_carRel++msg[1]),
				\cb_modAmp_bus,cb_busDepot.at(\cb_modAmp++msg[1]),
				\cb_modHz_bus,cb_busDepot.at(\cb_modHz++msg[1]),
				\cb_modAtk_bus,cb_busDepot.at(\cb_modAtk++msg[1]),
				\cb_modRel_bus,cb_busDepot.at(\cb_modRel++msg[1]),
				\cb_feedAmp_bus,cb_busDepot.at(\cb_feedAmp++msg[1]),
				\cb_rampDepth_bus,cb_busDepot.at(\cb_rampDepth++msg[1]),
				\cb_rampDec_bus,cb_busDepot.at(\cb_rampDec++msg[1]),
				\cb_click_bus,cb_busDepot.at(\cb_click++msg[1]),
				\cb_snap_bus,cb_busDepot.at(\cb_snap++msg[1]),
				\cb_AMD_bus,cb_busDepot.at(\cb_AMD++msg[1]),
				\cb_AMF_bus,cb_busDepot.at(\cb_AMF++msg[1]),
				\cb_EQF_bus,cb_busDepot.at(\cb_EQF++msg[1]),
				\cb_EQG_bus,cb_busDepot.at(\cb_EQG++msg[1]),
				\cb_LPfreq_bus,cb_busDepot.at(\cb_LPfreq++msg[1]),
				\cb_HPfreq_bus,cb_busDepot.at(\cb_HPfreq++msg[1]),
				\cb_filterQ_bus,cb_busDepot.at(\cb_filterQ++msg[1]),
				\cb_pan_bus,cb_busDepot.at(\cb_pan++msg[1]),
				\cb_brate_bus,cb_busDepot.at(\cb_brate++msg[1]),
				\cb_bcnt_bus,cb_busDepot.at(\cb_bcnt++msg[1]),
				\cb_SPTCH_bus,cb_busDepot.at(\cb_SPTCH++msg[1]),
				\cb_SCHNK_bus,cb_busDepot.at(\cb_SCHNK++msg[1]),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[msg[1]-1]);
			("triggering a thing "++(synthArray[msg[1]-1].nodeID)).postln;
		});

		hh_params = Dictionary.newFrom([
			\hh_amp,0.5,
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
			\hh_SCHNK,1,
		]);

		hh_params.keysValuesDo({ arg key,value;
			hh_busDepot.put(key++6,Bus.control(context.server));
			hh_busDepot.at(key++6).set(value);
			this.addCommand(key, "if", { arg msg;
				// NOTE: added a little debug here
				["setting",key,msg[1],msg[2],hh_busDepot.at(key++msg[1])].postln;
				hh_busDepot.at(key++msg[1]).set(msg[2]);
			});
		});

		this.addCommand("trig_hh", "i", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[msg[1]-1].notNil,{
				if (synthArray[msg[1]-1].isRunning,{
					synthArray[msg[1]-1].set(\kill_gate,-1.05);
				});
			});
			[(\hh_amp++msg[1]),hh_busDepot.at(\hh_amp++msg[1])].postln;
			// synthArray[msg[1]-1]=Synth.new("hh",hh_params.getPairs);
			synthArray[msg[1]-1]=Synth.new("hh",[
				\hh_amp_bus,hh_busDepot.at(\hh_amp++msg[1]).index.postln,
				\hh_carHz_bus,hh_busDepot.at(\hh_carHz++msg[1]),
				\hh_carAtk_bus,hh_busDepot.at(\hh_carAtk++msg[1]),
				\hh_carRel_bus,hh_busDepot.at(\hh_carRel++msg[1]),
				\hh_tremDepth_bus,hh_busDepot.at(\hh_tremDepth++msg[1]),
				\hh_tremHz_bus,hh_busDepot.at(\hh_tremHz++msg[1]),
				\hh_modAmp_bus,hh_busDepot.at(\hh_modAmp++msg[1]),
				\hh_modHz_bus,hh_busDepot.at(\hh_modHz++msg[1]),
				\hh_modAtk_bus,hh_busDepot.at(\hh_modAtk++msg[1]),
				\hh_modRel_bus,hh_busDepot.at(\hh_modRel++msg[1]),
				\hh_feedAmp_bus,hh_busDepot.at(\hh_feedAmp++msg[1]),
				\hh_AMD_bus,hh_busDepot.at(\hh_AMD++msg[1]),
				\hh_AMF_bus,hh_busDepot.at(\hh_AMF++msg[1]),
				\hh_LPfreq_bus,hh_busDepot.at(\hh_LPfreq++msg[1]),
				\hh_HPfreq_bus,hh_busDepot.at(\hh_HPfreq++msg[1]),
				\hh_filterQ_bus,hh_busDepot.at(\hh_filterQ++msg[1]),
				\hh_pan_bus,hh_busDepot.at(\hh_pan++msg[1]),
				\hh_brate_bus,hh_busDepot.at(\hh_brate++msg[1]),
				\hh_bcnt_bus,hh_busDepot.at(\hh_bcnt++msg[1]),
				\hh_SPTCH_bus,hh_busDepot.at(\hh_SPTCH++msg[1]),
				\hh_SCHNK_bus,hh_busDepot.at(\hh_SCHNK++msg[1]),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[msg[1]-1]);
			("triggering a thing "++(synthArray[msg[1]-1].nodeID)).postln;
		});

	}

	free {
		(0..9).do({arg i; synthArray[i].free});
		bd_busDepot.keysValuesDo({ arg key,value; value.free});
		sd_busDepot.keysValuesDo({ arg key,value; value.free});
		hh_busDepot.keysValuesDo({ arg key,value; value.free});
	}
}