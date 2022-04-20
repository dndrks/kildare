Engine_kildare : CroneEngine {
	var synthArray;
	var bd_params;
	var bd_busDepot;
	var sd_params;
	var sd_busDepot;
	var tm_params;
	var tm_busDepot;
	var cp_params;
	var cp_busDepot;
	var rs_params;
	var rs_busDepot;
	var cb_params;
	var cb_busDepot;
	var hh_params;
	var hh_busDepot;
	// var cy_params;
	// var cy_busDepot;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {

		synthArray = Array.newClear(10);
		bd_busDepot = Dictionary.new();
		sd_busDepot = Dictionary.new();
		tm_busDepot = Dictionary.new();
		cp_busDepot = Dictionary.new();
		rs_busDepot = Dictionary.new();
		cb_busDepot = Dictionary.new();
		hh_busDepot = Dictionary.new();
		// cy_busDepot = Dictionary.new();

		SynthDef("bd", {
			arg out = 0, kill_gate = 1,
			bd_amp_bus = 0, bd_carHz_bus = 0, bd_carAtk_bus = 0, bd_carRel_bus = 0,
			bd_modHz_bus = 0, bd_modAmp_bus = 0, bd_modAtk_bus = 0, bd_modRel_bus = 0, bd_feedAmp_bus = 0,
			bd_pan_bus = 0, bd_rampDepth_bus = 0, bd_rampDec_bus = 0,
			bd_SPTCH_bus = 0, bd_SCHNK_bus = 0,
			bd_AMD_bus = 0, bd_AMF_bus = 0,
			bd_EQF_bus = 0, bd_EQG_bus = 0, bd_brate_bus = 0, bd_bcnt_bus = 0,
			bd_LPfreq_bus = 0, bd_HPfreq_bus = 0, bd_filterQ_bus = 0;

			var bd_amp, bd_carHz, bd_carAtk, bd_carRel,
			bd_modHz, bd_modAmp, bd_modAtk, bd_modRel, bd_feedAmp,
			bd_pan, bd_rampDepth, bd_rampDec,
			bd_SPTCH, bd_SCHNK,
			bd_AMD, bd_AMF,
			bd_EQF, bd_EQG, bd_brate, bd_bcnt,
			bd_LPfreq, bd_HPfreq, bd_filterQ;

			var bd_car, bd_mod, bd_carEnv, bd_modEnv, bd_carRamp,
			bd_feedMod, bd_feedCar, bd_ampMod, bd_EQ, bd_click, bd_clicksound,
			mod_1;

			bd_amp=In.kr(bd_amp_bus,1);
			bd_carHz=In.kr(bd_carHz_bus,1);
			bd_carAtk=In.kr(bd_carAtk_bus,1);
			bd_carRel=In.kr(bd_carRel_bus,1);
			bd_modHz=In.kr(bd_modHz_bus,1);
			bd_modAmp=In.kr(bd_modAmp_bus,1);
			bd_modAtk=In.kr(bd_modAtk_bus,1);
			bd_modRel=In.kr(bd_modRel_bus,1);
			// bd_feedAmp=LinLin.kr(In.kr(bd_feedAmp_bus,1),0,127,0.0,10.0);
			bd_feedAmp=In.kr(bd_feedAmp_bus,1);
			bd_rampDepth=In.kr(bd_rampDepth_bus,1);
			bd_rampDec=In.kr(bd_rampDec_bus,1);
			bd_AMD=In.kr(bd_AMD_bus,1);
			bd_AMF=In.kr(bd_AMF_bus,1);
			bd_EQF=In.kr(bd_EQF_bus,1);
			bd_EQG=In.kr(bd_EQG_bus,1);
			bd_brate=In.kr(bd_brate_bus,1);
			bd_bcnt=In.kr(bd_bcnt_bus,1);
			bd_LPfreq=In.kr(bd_LPfreq_bus,1).lag3(1);
			bd_HPfreq=In.kr(bd_HPfreq_bus,1).lag3(1);
			bd_filterQ=In.kr(bd_filterQ_bus,1);
			bd_pan=In.kr(bd_pan_bus,1).lag2(0.1);
			bd_SPTCH=In.kr(bd_SPTCH_bus,1);
			bd_SCHNK=In.kr(bd_SCHNK_bus,1);

			bd_filterQ = LinLin.kr(bd_filterQ,0,100,2.0,0.001);
			bd_modAmp = LinLin.kr(bd_modAmp,0.0,1.0,0,127);
			bd_feedAmp = LinLin.kr(bd_feedAmp,0.0,1.0,0.0,10.0);
			bd_EQG = LinLin.kr(bd_EQG,0.0,1.0,0.0,10.0);
			bd_rampDepth = LinLin.kr(bd_rampDepth,0.0,1.0,0.0,2.0);
			bd_AMD = LinLin.kr(bd_AMD,0.0,1.0,0.0,2.0);

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

			bd_car = Squiz.ar(in:bd_car, pitchratio:bd_SPTCH, zcperchunk:bd_SCHNK, mul:1);
			bd_car = Decimator.ar(bd_car,bd_brate,bd_bcnt,1.0);

			bd_car = BPeakEQ.ar(in:bd_car,freq:bd_EQF,rq:1,db:bd_EQG,mul:1);
			bd_car = BLowPass.ar(in:bd_car,freq:bd_LPfreq, rq: bd_filterQ, mul:1);
			bd_car = RHPF.ar(in:bd_car,freq:bd_HPfreq, rq: bd_filterQ, mul:1);

			bd_car = Compander.ar(in:bd_car,control:bd_car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			bd_car = Pan2.ar(bd_car,bd_pan);
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
			sd_noise, sd_noiseEnv, sd_mix, sd_ampMod;

			sd_amp=In.kr(sd_amp_bus,1)/2;
			sd_carHz=In.kr(sd_carHz_bus,1);
			sd_carAtk=In.kr(sd_carAtk_bus,1);
			sd_carRel=In.kr(sd_carRel_bus,1);
			sd_modHz=In.kr(sd_modHz_bus,1);
			sd_modAmp=In.kr(sd_modAmp_bus,1);
			sd_modAtk=In.kr(sd_modAtk_bus,1);
			sd_modRel=In.kr(sd_modRel_bus,1);
			sd_noiseAmp=In.kr(sd_noiseAmp_bus,1)/2;
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
			sd_LPfreq=In.kr(sd_LPfreq_bus,1).lag3(1);
			sd_HPfreq=In.kr(sd_HPfreq_bus,1).lag3(1);
			sd_filterQ=In.kr(sd_filterQ_bus,1);
			sd_pan=In.kr(sd_pan_bus,1).lag2(0.1);
			sd_SPTCH=In.kr(sd_SPTCH_bus,1);
			sd_SCHNK=In.kr(sd_SCHNK_bus,1);

			sd_filterQ = LinLin.kr(sd_filterQ,0,100,2.0,0.001);
			sd_modEnv = EnvGen.kr(Env.perc(sd_modAtk, sd_modRel));
			sd_carRamp = EnvGen.kr(Env([1000, 0.000001], [sd_rampDec], curve: \exp));
			sd_carEnv = EnvGen.kr(Env.perc(sd_carAtk, sd_carRel),gate: kill_gate);
			sd_modAmp = LinLin.kr(sd_modAmp,0.0,1.0,0,127);
			sd_feedMod = SinOsc.ar(sd_modHz, mul:sd_modAmp*100) * sd_modEnv;
			sd_feedAmp = LinLin.kr(sd_feedAmp,0,1,0.0,10.0);
			sd_EQG = LinLin.kr(sd_EQG,0.0,1.0,0.0,10.0);
			sd_feedAmp = sd_feedAmp * sd_modAmp; // 220224
			sd_rampDepth = LinLin.kr(sd_rampDepth,0.0,1.0,0.0,2.0);
			sd_feedCar = SinOsc.ar(sd_carHz + sd_feedMod + (sd_carRamp*sd_rampDepth)) * sd_carEnv * (sd_feedAmp/sd_modAmp * 127);
			sd_mod = SinOsc.ar(sd_modHz + sd_feedCar, mul:sd_modAmp*100) * sd_modEnv;
			sd_car = SinOsc.ar(sd_carHz + sd_mod + (sd_carRamp*sd_rampDepth)) * sd_carEnv * sd_amp;

			sd_noiseEnv = EnvGen.kr(Env.perc(sd_noiseAtk,sd_noiseRel),gate: kill_gate);
			sd_noise = BPF.ar(WhiteNoise.ar,8000,1.3) * (sd_noiseAmp*sd_noiseEnv);
			sd_noise = BPeakEQ.ar(in:sd_noise,freq:sd_EQF,rq:1,db:sd_EQG,mul:1);
			sd_noise = BLowPass.ar(in:sd_noise,freq:sd_LPfreq, rq: sd_filterQ, mul:1);
			sd_noise = RHPF.ar(in:sd_noise,freq:sd_HPfreq, rq: sd_filterQ, mul:1);

			sd_AMD = LinLin.kr(sd_AMD,0.0,1.0,0.0,2.0);
			sd_ampMod = SinOsc.ar(freq:sd_AMF,mul:(sd_AMD/2),add:1);
			sd_car = sd_car * sd_ampMod;

			sd_car = Squiz.ar(in:sd_car, pitchratio:sd_SPTCH, zcperchunk:sd_SCHNK, mul:1);
        	sd_noise = Squiz.ar(in:sd_noise, pitchratio:sd_SPTCH, zcperchunk:sd_SCHNK*100, mul:1);

			sd_car = Decimator.ar(sd_car,sd_brate,sd_bcnt,1.0);

			sd_car = BPeakEQ.ar(in:sd_car,freq:sd_EQF,rq:1,db:sd_EQG,mul:1);
			sd_car = BLowPass.ar(in:sd_car,freq:sd_LPfreq, rq: sd_filterQ, mul:1);
			sd_car = RHPF.ar(in:sd_car,freq:sd_HPfreq, rq: sd_filterQ, mul:1);

			// sd_mix = Decimator.ar(sd_car,sd_brate,sd_bcnt,1.0);
			sd_mix = sd_car;
			sd_mix = Compander.ar(in:sd_mix,control:sd_mix, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			sd_noise = Compander.ar(in:sd_noise,control:sd_noise, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			Out.ar(out, Pan2.ar(sd_mix,sd_pan));
			Out.ar(out, Pan2.ar(sd_noise,sd_pan));
			FreeSelf.kr(Done.kr(sd_carEnv) * Done.kr(sd_noiseEnv));
		}).add;

		SynthDef("tm", {

			arg out = 0, kill_gate = 1,
			tm_carHz_bus = 0, tm_modHz_bus = 0, tm_modAmp_bus = 0, tm_modAtk_bus = 0, tm_modRel_bus = 0, tm_feedAmp_bus = 0,
			tm_carAtk_bus = 0, tm_carRel_bus = 0, tm_amp_bus = 0,
			tm_click_bus = 0,
			tm_SPTCH_bus = 0, tm_SCHNK_bus = 0,
			tm_pan_bus = 0, tm_rampDepth_bus = 0, tm_rampDec_bus = 0, tm_AMD_bus = 0, tm_AMF_bus = 0,
			tm_EQF_bus = 0, tm_EQG_bus = 0, tm_brate_bus = 0, tm_bcnt_bus = 0,
			tm_LPfreq_bus = 0, tm_HPfreq_bus = 0, tm_filterQ_bus = 0;

			var tm_carHz, tm_modHz, tm_modAmp, tm_modAtk, tm_modRel, tm_feedAmp,
			tm_carAtk, tm_carRel, tm_amp,
			tm_click,
			tm_SPTCH, tm_SCHNK,
			tm_pan, tm_rampDepth, tm_rampDec, tm_AMD, tm_AMF,
			tm_EQF, tm_EQG, tm_brate, tm_bcnt,
			tm_LPfreq, tm_HPfreq, tm_filterQ;

			var tm_car, tm_mod, tm_carEnv, tm_modEnv, tm_carRamp, tm_feedMod,
			tm_feedCar, tm_ampMod, tm_EQ, tm_clicksound,
			mod_1;

			tm_amp=In.kr(tm_amp_bus,1)*0.66;
			tm_carHz=In.kr(tm_carHz_bus,1);
			tm_carAtk=In.kr(tm_carAtk_bus,1);
			tm_carRel=In.kr(tm_carRel_bus,1);
			tm_modHz=In.kr(tm_modHz_bus,1);
			tm_modAmp=In.kr(tm_modAmp_bus,1);
			tm_modAtk=In.kr(tm_modAtk_bus,1);
			tm_modRel=In.kr(tm_modRel_bus,1);
			tm_feedAmp=In.kr(tm_feedAmp_bus,1);
			tm_rampDepth=In.kr(tm_rampDepth_bus,1);
			tm_rampDec=In.kr(tm_rampDec_bus,1);
			tm_AMD=In.kr(tm_AMD_bus,1);
			tm_AMF=In.kr(tm_AMF_bus,1);
			tm_EQF=In.kr(tm_EQF_bus,1);
			tm_EQG=In.kr(tm_EQG_bus,1);
			tm_brate=In.kr(tm_brate_bus,1);
			tm_bcnt=In.kr(tm_bcnt_bus,1);
			tm_click=In.kr(tm_click_bus,1);
			tm_LPfreq=In.kr(tm_LPfreq_bus,1).lag3(1);
			tm_HPfreq=In.kr(tm_HPfreq_bus,1).lag3(1);
			tm_filterQ=In.kr(tm_filterQ_bus,1);
			tm_pan=In.kr(tm_pan_bus,1).lag2(0.1);
			tm_SPTCH=In.kr(tm_SPTCH_bus,1);
			tm_SCHNK=In.kr(tm_SCHNK_bus,1);

			tm_filterQ = LinLin.kr(tm_filterQ,0,100,2.0,0.001);
			tm_modAmp = LinLin.kr(tm_modAmp,0.0,1.0,0,127);
			tm_feedAmp = LinLin.kr(tm_feedAmp,0.0,1.0,0.0,10.0);
			tm_EQG = LinLin.kr(tm_EQG,0.0,1.0,0.0,10.0);
			tm_rampDepth = LinLin.kr(tm_rampDepth,0.0,1.0,0.0,2.0);
			tm_AMD = LinLin.kr(tm_AMD,0,1.0,0.0,2.0);

			tm_modEnv = EnvGen.kr(Env.perc(tm_modAtk, tm_modRel));
			tm_carRamp = EnvGen.kr(Env([600, 0.000001], [tm_rampDec], curve: \lin));
			tm_carEnv = EnvGen.kr(Env.perc(tm_carAtk, tm_carRel), gate: kill_gate, doneAction:2);

			mod_1 = SinOscFB.ar(
				tm_modHz,
				tm_feedAmp,
				tm_modAmp*10
			)* tm_modEnv;

			tm_car = SinOsc.ar(tm_carHz + (mod_1) + (tm_carRamp*tm_rampDepth)) * tm_carEnv * tm_amp;

			tm_ampMod = SinOsc.ar(freq:tm_AMF,mul:tm_AMD,add:1);
			tm_clicksound = LPF.ar(Impulse.ar(0.003),16000,tm_click) * EnvGen.kr(envelope: Env.perc(tm_carAtk, 0.2),gate: kill_gate);
			tm_car = (tm_car + tm_clicksound) * tm_ampMod;
			tm_car = Squiz.ar(in:tm_car, pitchratio:tm_SPTCH, zcperchunk:tm_SCHNK, mul:1);
			tm_car = Decimator.ar(tm_car,tm_brate,tm_bcnt,1.0);

			tm_car = BPeakEQ.ar(in:tm_car,freq:tm_EQF,rq:1,db:tm_EQG,mul:1);

			tm_car = BLowPass.ar(in:tm_car,freq:tm_LPfreq, rq: tm_filterQ, mul:1);
			tm_car = RHPF.ar(in:tm_car,freq:tm_HPfreq, rq: tm_filterQ, mul:1);

			tm_car = Compander.ar(in:tm_car,control:tm_car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

			Out.ar(out, Pan2.ar(tm_car,tm_pan));
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
			mod_1,mod_2;

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
			cp_LPfreq=In.kr(cp_LPfreq_bus,1).lag3(1);
			cp_HPfreq=In.kr(cp_HPfreq_bus,1).lag3(1);
			cp_filterQ=In.kr(cp_filterQ_bus,1);
			cp_pan=In.kr(cp_pan_bus,1).lag2(0.1);
			cp_SPTCH=In.kr(cp_SPTCH_bus,1);
			cp_SCHNK=In.kr(cp_SCHNK_bus,1);

			cp_filterQ = LinLin.kr(cp_filterQ,0,100,2.0,0.001);
			cp_modAmp = LinLin.kr(cp_modAmp,0.0,1.0,0,127);
			cp_feedAmp = LinLin.kr(cp_feedAmp,0.0,1.0,0.0,10.0);
			cp_EQG = LinLin.kr(cp_EQG,0.0,1.0,0.0,10.0);
			cp_AMD = LinLin.kr(cp_AMD,0,1.0,0.0,2.0);

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

			cp_car = Decimator.ar(cp_car,cp_brate,cp_bcnt,1.0);

			cp_car = BPeakEQ.ar(in:cp_car,freq:cp_EQF,rq:1,db:cp_EQG,mul:1);

			cp_car = BLowPass.ar(in:cp_car,freq:cp_LPfreq, rq: cp_filterQ, mul:1);
			cp_car = RHPF.ar(in:cp_car,freq:cp_HPfreq, rq: cp_filterQ, mul:1);

			cp_car = cp_car.softclip;
			Out.ar(out, Pan2.ar(cp_car,cp_pan));
			FreeSelf.kr(Done.kr(cp_modEnv) * Done.kr(cp_carEnv));
		}).add;

		SynthDef("rs", {
			arg out = 0, kill_gate = 1,
			rs_carHz_bus = 0,
			rs_modHz_bus = 0, rs_modAmp_bus = 0,
			rs_carAtk_bus = 0, rs_carRel_bus = 0, rs_amp_bus = 0,
			rs_pan_bus = 0, rs_rampDepth_bus = 0, rs_rampDec_bus = 0, rs_AMD_bus = 0, rs_AMF_bus = 0,
			rs_EQF_bus = 0, rs_EQG_bus = 0, rs_brate_bus = 0, rs_bcnt_bus = 0,
			rs_sdAmp_bus = 0, rs_sdRel_bus = 0, rs_sdAtk_bus = 0,

			rs_LPfreq_bus = 0, rs_HPfreq_bus = 0, rs_filterQ_bus = 0,
			rs_SPTCH_bus = 0, rs_SCHNK_bus = 0;

			var rs_carHz,
			rs_modHz, rs_modAmp,
			rs_carAtk, rs_carRel, rs_amp,
			rs_pan, rs_rampDepth, rs_rampDec, rs_AMD, rs_AMF,
			rs_EQF, rs_EQG, rs_brate, rs_bcnt,
			rs_sdAmp, rs_sdRel, rs_sdAtk,

			rs_LPfreq, rs_HPfreq, rs_filterQ,
			rs_SPTCH, rs_SCHNK;

			var rs_car, rs_mod, rs_carEnv, rs_modEnv, rs_carRamp, rs_feedMod, rs_feedCar, rs_ampMod, rs_EQ,
			mod_1,mod_2,rs_feedAmp,rs_feedAMP, sd_modHz,
			sd_car, sd_mod, sd_carEnv, sd_modEnv, sd_carRamp, sd_feedMod, sd_feedCar, sd_noise, sd_noiseEnv,
			sd_mix;

			rs_amp=In.kr(rs_amp_bus,1)*0.35;
			rs_carHz=In.kr(rs_carHz_bus,1);
			rs_carAtk=In.kr(rs_carAtk_bus,1);
			rs_carRel=In.kr(rs_carRel_bus,1);
			rs_modHz=In.kr(rs_modHz_bus,1);
			rs_modAmp=In.kr(rs_modAmp_bus,1);
			rs_rampDepth=In.kr(rs_rampDepth_bus,1);
			rs_rampDec=In.kr(rs_rampDec_bus,1);
			rs_sdAmp=In.kr(rs_sdAmp_bus,1);
			rs_sdAtk=In.kr(rs_sdAtk_bus,1);
			rs_sdRel=In.kr(rs_sdRel_bus,1);
			rs_AMD=In.kr(rs_AMD_bus,1);
			rs_AMF=In.kr(rs_AMF_bus,1);
			rs_EQF=In.kr(rs_EQF_bus,1);
			rs_EQG=In.kr(rs_EQG_bus,1);
			rs_brate=In.kr(rs_brate_bus,1);
			rs_bcnt=In.kr(rs_bcnt_bus,1);
			rs_LPfreq=In.kr(rs_LPfreq_bus,1).lag3(1);
			rs_HPfreq=In.kr(rs_HPfreq_bus,1).lag3(1);
			rs_filterQ=In.kr(rs_filterQ_bus,1);
			rs_pan=In.kr(rs_pan_bus,1).lag2(0.1);
			rs_SPTCH=In.kr(rs_SPTCH_bus,1);
			rs_SCHNK=In.kr(rs_SCHNK_bus,1);

			rs_filterQ = LinLin.kr(rs_filterQ,0,100,2.0,0.001);
			rs_modAmp = LinLin.kr(rs_modAmp,0.0,1.0,0,127);
			// rs_feedAmp = LinLin.kr(rs_feedAmp,0.0,1.0,0.0,10.0);
			rs_EQG = LinLin.kr(rs_EQG,0.0,1.0,0.0,10.0);
			rs_rampDepth = LinLin.kr(rs_rampDepth,0.0,1.0,0.0,2.0);
			rs_AMD = LinLin.kr(rs_AMD,0,1.0,0.0,2.0);

			rs_feedAmp = rs_modAmp.linlin(0, 127, 0, 3);
			rs_feedAMP = rs_modAmp.linlin(0, 127, 0, 4);
			rs_carRamp = EnvGen.kr(Env([600, 0.000001], [rs_rampDec], curve: \lin));
			rs_carEnv = EnvGen.kr(Env.perc(rs_carAtk, rs_carRel),gate: kill_gate);

			mod_2 = SinOscFB.ar(
				rs_modHz*16,
				rs_feedAmp,
				rs_modAmp*10
			)* 1;

			mod_1 = SinOscFB.ar(
				rs_modHz+mod_2,
				rs_feedAmp,
				rs_modAmp*10
			)* 1;

			rs_car = SinOscFB.ar(rs_carHz + (mod_1+mod_2) + (rs_carRamp*rs_rampDepth),rs_feedAMP) * rs_carEnv * rs_amp;

			rs_ampMod = SinOsc.ar(freq:rs_AMF,mul:rs_AMD,add:1);
			rs_car = (rs_car+(LPF.ar(Impulse.ar(0.003),16000,1)*rs_amp)) * rs_ampMod;
			rs_car = Squiz.ar(in:rs_car, pitchratio:rs_SPTCH, zcperchunk:rs_SCHNK, mul:1);
			rs_car = Decimator.ar(Pan2.ar(rs_car,rs_pan),rs_brate,rs_bcnt,1.0);

			rs_car = BPeakEQ.ar(in:rs_car,freq:rs_EQF,rq:1,db:rs_EQG,mul:1);

			rs_car = BLowPass.ar(in:rs_car,freq:rs_LPfreq, rq: rs_filterQ, mul:1);
			rs_car = RHPF.ar(in:rs_car,freq:rs_HPfreq, rq: rs_filterQ, mul:1);

			rs_car = LPF.ar(rs_car,12000,1);
			rs_car = rs_car.softclip;
			rs_car = Compander.ar(in:rs_car,control:rs_car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

			Out.ar(out, Pan2.ar(rs_car,rs_pan));

			sd_modHz = rs_carHz*2.52;
			sd_modEnv = EnvGen.kr(Env.perc(rs_carAtk, rs_carRel));
			sd_carRamp = EnvGen.kr(Env([1000, 0.000001], [rs_rampDec], curve: \exp));
			sd_carEnv = EnvGen.kr(Env.perc(rs_sdAtk, rs_sdRel),gate:kill_gate);
			sd_feedMod = SinOsc.ar(rs_modHz, mul:rs_modAmp*100) * sd_modEnv;
			sd_feedCar = SinOsc.ar(rs_carHz + sd_feedMod + (rs_carRamp*rs_rampDepth)) * sd_carEnv * (rs_feedAmp*10);
			sd_mod = SinOsc.ar(rs_modHz + sd_feedCar, mul:rs_modAmp) * sd_modEnv;
			sd_car = SinOsc.ar(rs_carHz + sd_mod + (rs_carRamp*rs_rampDepth)) * sd_carEnv * rs_sdAmp;
			sd_car = sd_car * rs_ampMod;
			sd_mix = Squiz.ar(in:sd_car, pitchratio:rs_SPTCH, zcperchunk:rs_SCHNK, mul:1);
			sd_mix = Decimator.ar(sd_mix,rs_brate,rs_bcnt,1.0);
			sd_mix = BPeakEQ.ar(in:sd_mix,freq:rs_EQF,rq:1,db:rs_EQG,mul:1);
			sd_mix = BLowPass.ar(in:sd_mix,freq:rs_LPfreq, rq: rs_filterQ, mul:1);
			sd_mix = RHPF.ar(in:sd_mix,freq:rs_HPfreq, rq: rs_filterQ, mul:1);

			sd_mix = sd_mix.softclip;
			sd_mix = Compander.ar(in:sd_mix,control:sd_mix, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);

			Out.ar(out, Pan2.ar(sd_mix,rs_pan));

			FreeSelf.kr(Done.kr(sd_carEnv) * Done.kr(rs_carEnv));
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
			sig,cb_1,cb_2,klank_env,other_mod1,other_mod2;

			cb_amp=In.kr(cb_amp_bus,1)*0.6;
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

			cb_filterQ = LinLin.kr(cb_filterQ,0,100,2.0,0.001);
			cb_feedAmp = LinLin.kr(cb_feedAmp,0.0,1.0,1.0,3.0);
			cb_EQG = LinLin.kr(cb_EQG,0.0,1.0,0.0,10.0);
			cb_rampDepth = LinLin.kr(cb_rampDepth,0.0,1.0,0.0,2.0);
			cb_AMD = LinLin.kr(cb_AMD,0,1.0,0.0,2.0);
			cb_snap = LinLin.kr(cb_snap,0.0,1.0,0.0,10.0);

			cb_modEnv = EnvGen.kr(Env.perc(cb_modAtk, cb_modRel), gate:kill_gate);
			cb_carRamp = EnvGen.kr(Env([600, 0.000001], [cb_rampDec], curve: \lin));
			cb_carEnv = EnvGen.kr(Env.perc(cb_carAtk, cb_carRel),gate: kill_gate);

			cb_1 = LFPulse.ar((cb_carHz) + (cb_carRamp*cb_rampDepth)) * cb_carEnv * cb_amp;
			cb_2 = SinOscFB.ar((cb_carHz*1.5085)+ (cb_carRamp*cb_rampDepth),cb_feedAmp) * cb_carEnv * cb_amp;
			cb_ampMod = SinOsc.ar(freq:cb_AMF,mul:cb_AMD,add:1);
			cb_1 = (cb_1+(LPF.ar(Impulse.ar(0.003),16000,1)*cb_snap)) * cb_ampMod;
			cb_1 = (cb_1*0.33)+(cb_2*0.33);
			cb_1 = Squiz.ar(in:cb_1, pitchratio:cb_SPTCH, zcperchunk:cb_SCHNK, mul:1);
			cb_1 = Decimator.ar(cb_1,cb_brate,cb_bcnt,1.0);
			cb_1 = BPeakEQ.ar(in:cb_1,freq:cb_EQF,rq:1,db:cb_EQG,mul:1);
			cb_1 = BLowPass.ar(in:cb_1,freq:cb_LPfreq, rq: cb_filterQ, mul:1);
			cb_1 = RHPF.ar(in:cb_1,freq:cb_HPfreq, rq: cb_filterQ, mul:1);
			cb_1 = Compander.ar(in:cb_1,control:cb_1, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			Out.ar(out, Pan2.ar(cb_1,cb_pan));
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
			hh_ampMod;

			hh_amp=In.kr(hh_amp_bus,1)*0.85;
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
			hh_LPfreq=In.kr(hh_LPfreq_bus,1).lag3(1);
			hh_HPfreq=In.kr(hh_HPfreq_bus,1).lag3(1);
			hh_filterQ=In.kr(hh_filterQ_bus,1);
			hh_pan=In.kr(hh_pan_bus,1).lag2(0.1);
			hh_SPTCH=In.kr(hh_SPTCH_bus,1);
			hh_SCHNK=In.kr(hh_SCHNK_bus,1);

			hh_filterQ = LinLin.kr(hh_filterQ,0,100,2.0,0.001);
			hh_modAmp = LinLin.kr(hh_modAmp,0.0,1.0,0,127);
			hh_feedAmp = LinLin.kr(hh_feedAmp,0.0,1.0,0.0,10.0);
			hh_EQG = LinLin.kr(hh_EQG,0.0,1.0,0.0,10.0);
			hh_tremDepth = LinLin.kr(hh_tremDepth,0.0,100,0.0,1.0);
			hh_AMD = LinLin.kr(hh_AMD,0,1.0,0.0,2.0);

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
			hh_car = Decimator.ar(hh_car,hh_brate,hh_bcnt,1.0);
			hh_car = BPeakEQ.ar(in:hh_car,freq:hh_EQF,rq:1,db:hh_EQG,mul:1);

			hh_car = BLowPass.ar(in:hh_car,freq:hh_LPfreq, rq: hh_filterQ, mul:1);
			hh_car = RHPF.ar(in:hh_car,freq:hh_HPfreq, rq: hh_filterQ, mul:1);
			hh_car = Compander.ar(in:hh_car,control:hh_car, thresh:0.3, slopeBelow:1, slopeAbove:0.1, clampTime:0.01, relaxTime:0.01);
			Out.ar(out, Pan2.ar(hh_car,hh_pan));
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
			\bd_LPfreq,19000,
			\bd_HPfreq,0,
			\bd_filterQ,1,
		]);

		bd_params.keysValuesDo({ arg key,value;
			bd_busDepot.put(key++1,Bus.control(context.server));
			bd_busDepot.at(key++1).set(value);
			this.addCommand(key, "f", { arg msg;
				// NOTE: added a little debug here
				// ["setting",key,1,msg[1],bd_busDepot.at(key++1)].postln;
				bd_busDepot.at(key++1).set(msg[1]);
			});
		});

		this.addCommand("trig_bd", "", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[1-1].notNil,{
				if (synthArray[1-1].isRunning,{
					synthArray[1-1].set(\kill_gate,-1.05);
				});
			});
			// [(\bd_amp++1),bd_busDepot.at(\bd_amp++1)].postln;
			// synthArray[1-1]=Synth.new("hh",bd_params.getPairs);
			synthArray[1-1]=Synth.new("bd",[
				\bd_amp_bus,bd_busDepot.at(\bd_amp++1),
				\bd_carHz_bus,bd_busDepot.at(\bd_carHz++1),
				\bd_carAtk_bus,bd_busDepot.at(\bd_carAtk++1),
				\bd_carRel_bus,bd_busDepot.at(\bd_carRel++1),
				\bd_modAmp_bus,bd_busDepot.at(\bd_modAmp++1),
				\bd_modHz_bus,bd_busDepot.at(\bd_modHz++1),
				\bd_modAtk_bus,bd_busDepot.at(\bd_modAtk++1),
				\bd_modRel_bus,bd_busDepot.at(\bd_modRel++1),
				\bd_feedAmp_bus,bd_busDepot.at(\bd_feedAmp++1),
				\bd_rampDepth_bus,bd_busDepot.at(\bd_rampDepth++1),
				\bd_rampDec_bus,bd_busDepot.at(\bd_rampDec++1),
				\bd_AMD_bus,bd_busDepot.at(\bd_AMD++1),
				\bd_AMF_bus,bd_busDepot.at(\bd_AMF++1),
				\bd_EQF_bus,bd_busDepot.at(\bd_EQF++1),
				\bd_EQG_bus,bd_busDepot.at(\bd_EQG++1),
				\bd_LPfreq_bus,bd_busDepot.at(\bd_LPfreq++1),
				\bd_HPfreq_bus,bd_busDepot.at(\bd_HPfreq++1),
				\bd_filterQ_bus,bd_busDepot.at(\bd_filterQ++1),
				\bd_pan_bus,bd_busDepot.at(\bd_pan++1),
				\bd_brate_bus,bd_busDepot.at(\bd_brate++1),
				\bd_bcnt_bus,bd_busDepot.at(\bd_bcnt++1),
				\bd_SPTCH_bus,bd_busDepot.at(\bd_SPTCH++1),
				\bd_SCHNK_bus,bd_busDepot.at(\bd_SCHNK++1),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[1-1]);
			// ("triggering a thing "++(synthArray[1-1].nodeID)).postln;
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
			this.addCommand(key, "f", { arg msg;
				// NOTE: added a little debug here
				// ["setting",key,2,msg[1],sd_busDepot.at(key++2)].postln;
				sd_busDepot.at(key++2).set(msg[1]);
			});
		});

		this.addCommand("trig_sd", "", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[2-1].notNil,{
				if (synthArray[2-1].isRunning,{
					synthArray[2-1].set(\kill_gate,-1.05);
				});
			});
			// [(\sd_amp++2),sd_busDepot.at(\sd_amp++2)].postln;
			// synthArray[2-1]=Synth.new("hh",sd_params.getPairs);
			synthArray[2-1]=Synth.new("sd",[
				\sd_amp_bus,sd_busDepot.at(\sd_amp++2),
				\sd_carHz_bus,sd_busDepot.at(\sd_carHz++2),
				\sd_carAtk_bus,sd_busDepot.at(\sd_carAtk++2),
				\sd_carRel_bus,sd_busDepot.at(\sd_carRel++2),
				\sd_modAmp_bus,sd_busDepot.at(\sd_modAmp++2),
				\sd_modHz_bus,sd_busDepot.at(\sd_modHz++2),
				\sd_modAtk_bus,sd_busDepot.at(\sd_modAtk++2),
				\sd_modRel_bus,sd_busDepot.at(\sd_modRel++2),
				\sd_noiseAmp_bus,sd_busDepot.at(\sd_noiseAmp++2),
				\sd_noiseAtk_bus,sd_busDepot.at(\sd_noiseAtk++2),
				\sd_noiseRel_bus,sd_busDepot.at(\sd_noiseRel++2),
				\sd_feedAmp_bus,sd_busDepot.at(\sd_feedAmp++2),
				\sd_rampDepth_bus,sd_busDepot.at(\sd_rampDepth++2),
				\sd_rampDec_bus,sd_busDepot.at(\sd_rampDec++2),
				\sd_click_bus,sd_busDepot.at(\sd_click++2),
				\sd_AMD_bus,sd_busDepot.at(\sd_AMD++2),
				\sd_AMF_bus,sd_busDepot.at(\sd_AMF++2),
				\sd_EQF_bus,sd_busDepot.at(\sd_EQF++2),
				\sd_EQG_bus,sd_busDepot.at(\sd_EQG++2),
				\sd_LPfreq_bus,sd_busDepot.at(\sd_LPfreq++2),
				\sd_HPfreq_bus,sd_busDepot.at(\sd_HPfreq++2),
				\sd_filterQ_bus,sd_busDepot.at(\sd_filterQ++2),
				\sd_pan_bus,sd_busDepot.at(\sd_pan++2),
				\sd_brate_bus,sd_busDepot.at(\sd_brate++2),
				\sd_bcnt_bus,sd_busDepot.at(\sd_bcnt++2),
				\sd_SPTCH_bus,sd_busDepot.at(\sd_SPTCH++2),
				\sd_SCHNK_bus,sd_busDepot.at(\sd_SCHNK++2),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[2-1]);
			// ("triggering a thing "++(synthArray[2-1].nodeID)).postln;
		});

		tm_params = Dictionary.newFrom([
			\tm_amp,1,
			\tm_carHz,87.3,
			\tm_carAtk,0,
			\tm_carRel,0.3,
			\tm_modHz,174.6,
			\tm_modAmp,100,
			\tm_modAtk,0,
			\tm_modRel,0.2,
			\tm_feedAmp,21,
			\tm_pan,0,
			\tm_rampDepth,0.3,
			\tm_rampDec,0.13,
			\tm_SPTCH,1,
			\tm_SCHNK,1,
			\tm_AMD,0,
			\tm_AMF,2698.8,
			\tm_EQF,6000,
			\tm_EQG,0,
			\tm_brate,24000,
			\tm_bcnt,24,
			\tm_click,1,
			\tm_LPfreq,19000,
			\tm_HPfreq,0,
			\tm_filterQ,0.5,
		]);

		tm_params.keysValuesDo({ arg key,value;
			tm_busDepot.put(key++3,Bus.control(context.server));
			tm_busDepot.at(key++3).set(value);
			this.addCommand(key, "f", { arg msg;
				// NOTE: added a little debug here
				// ["setting",key,3,msg[1],tm_busDepot.at(key++3)].postln;
				tm_busDepot.at(key++3).set(msg[1]);
			});
		});

		this.addCommand("trig_tm", "", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[3-1].notNil,{
				if (synthArray[3-1].isRunning,{
					synthArray[3-1].set(\kill_gate,-1.05);
				});
			});
			// [(\tm_amp++3),tm_busDepot.at(\tm_amp++3)].postln;
			// synthArray[3-1]=Synth.new("hh",tm_params.getPairs);
			synthArray[3-1]=Synth.new("tm",[
				\tm_amp_bus,tm_busDepot.at(\tm_amp++3),
				\tm_carHz_bus,tm_busDepot.at(\tm_carHz++3),
				\tm_carAtk_bus,tm_busDepot.at(\tm_carAtk++3),
				\tm_carRel_bus,tm_busDepot.at(\tm_carRel++3),
				\tm_modAmp_bus,tm_busDepot.at(\tm_modAmp++3),
				\tm_modHz_bus,tm_busDepot.at(\tm_modHz++3),
				\tm_modAtk_bus,tm_busDepot.at(\tm_modAtk++3),
				\tm_modRel_bus,tm_busDepot.at(\tm_modRel++3),
				\tm_feedAmp_bus,tm_busDepot.at(\tm_feedAmp++3),
				\tm_rampDepth_bus,tm_busDepot.at(\tm_rampDepth++3),
				\tm_rampDec_bus,tm_busDepot.at(\tm_rampDec++3),
				\tm_click_bus,tm_busDepot.at(\tm_click++3),
				\tm_AMD_bus,tm_busDepot.at(\tm_AMD++3),
				\tm_AMF_bus,tm_busDepot.at(\tm_AMF++3),
				\tm_EQF_bus,tm_busDepot.at(\tm_EQF++3),
				\tm_EQG_bus,tm_busDepot.at(\tm_EQG++3),
				\tm_LPfreq_bus,tm_busDepot.at(\tm_LPfreq++3),
				\tm_HPfreq_bus,tm_busDepot.at(\tm_HPfreq++3),
				\tm_filterQ_bus,tm_busDepot.at(\tm_filterQ++3),
				\tm_pan_bus,tm_busDepot.at(\tm_pan++3),
				\tm_brate_bus,tm_busDepot.at(\tm_brate++3),
				\tm_bcnt_bus,tm_busDepot.at(\tm_bcnt++3),
				\tm_SPTCH_bus,tm_busDepot.at(\tm_SPTCH++3),
				\tm_SCHNK_bus,tm_busDepot.at(\tm_SCHNK++3),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[3-1]);
			// ("triggering a thing "++(synthArray[3-1].nodeID)).postln;
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
			this.addCommand(key, "f", { arg msg;
				// NOTE: added a little debug here
				// ["setting",key,4,msg[1],cp_busDepot.at(key++4)].postln;
				cp_busDepot.at(key++4).set(msg[1]);
			});
		});

		this.addCommand("trig_cp", "", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[4-1].notNil,{
				if (synthArray[4-1].isRunning,{
					synthArray[4-1].set(\kill_gate,-1.05);
				});
			});
			// [(\cp_amp++4),cp_busDepot.at(\cp_amp++4)].postln;
			// synthArray[4-1]=Synth.new("hh",cp_params.getPairs);
			synthArray[4-1]=Synth.new("cp",[
				\cp_amp_bus,cp_busDepot.at(\cp_amp++4),
				\cp_carHz_bus,cp_busDepot.at(\cp_carHz++4),
				\cp_carRel_bus,cp_busDepot.at(\cp_carRel++4),
				\cp_modAmp_bus,cp_busDepot.at(\cp_modAmp++4),
				\cp_modHz_bus,cp_busDepot.at(\cp_modHz++4),
				\cp_modRel_bus,cp_busDepot.at(\cp_modRel++4),
				\cp_feedAmp_bus,cp_busDepot.at(\cp_feedAmp++4),
				\cp_click_bus,cp_busDepot.at(\cp_click++4),
				\cp_AMD_bus,cp_busDepot.at(\cp_AMD++4),
				\cp_AMF_bus,cp_busDepot.at(\cp_AMF++4),
				\cp_EQF_bus,cp_busDepot.at(\cp_EQF++4),
				\cp_EQG_bus,cp_busDepot.at(\cp_EQG++4),
				\cp_LPfreq_bus,cp_busDepot.at(\cp_LPfreq++4),
				\cp_HPfreq_bus,cp_busDepot.at(\cp_HPfreq++4),
				\cp_filterQ_bus,cp_busDepot.at(\cp_filterQ++4),
				\cp_pan_bus,cp_busDepot.at(\cp_pan++4),
				\cp_brate_bus,cp_busDepot.at(\cp_brate++4),
				\cp_bcnt_bus,cp_busDepot.at(\cp_bcnt++4),
				\cp_SPTCH_bus,cp_busDepot.at(\cp_SPTCH++4),
				\cp_SCHNK_bus,cp_busDepot.at(\cp_SCHNK++4),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[4-1]);
			// ("triggering a thing "++(synthArray[4-1].nodeID)).postln;
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
			this.addCommand(key, "f", { arg msg;
				// NOTE: added a little debug here
				// ["setting",key,5,msg[1],cb_busDepot.at(key++5)].postln;
				cb_busDepot.at(key++5).set(msg[1]);
			});
		});

		this.addCommand("trig_cb", "", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[5-1].notNil,{
				if (synthArray[5-1].isRunning,{
					synthArray[5-1].set(\kill_gate,-1.05);
				});
			});
			// [(\cb_amp++5),cb_busDepot.at(\cb_amp++5)].postln;
			// synthArray[5-1]=Synth.new("hh",cb_params.getPairs);
			synthArray[5-1]=Synth.new("cb",[
				\cb_amp_bus,cb_busDepot.at(\cb_amp++5),
				\cb_carHz_bus,cb_busDepot.at(\cb_carHz++5),
				\cb_carAtk_bus,cb_busDepot.at(\cb_carAtk++5),
				\cb_carRel_bus,cb_busDepot.at(\cb_carRel++5),
				\cb_modAmp_bus,cb_busDepot.at(\cb_modAmp++5),
				\cb_modHz_bus,cb_busDepot.at(\cb_modHz++5),
				\cb_modAtk_bus,cb_busDepot.at(\cb_modAtk++5),
				\cb_modRel_bus,cb_busDepot.at(\cb_modRel++5),
				\cb_feedAmp_bus,cb_busDepot.at(\cb_feedAmp++5),
				\cb_rampDepth_bus,cb_busDepot.at(\cb_rampDepth++5),
				\cb_rampDec_bus,cb_busDepot.at(\cb_rampDec++5),
				\cb_click_bus,cb_busDepot.at(\cb_click++5),
				\cb_snap_bus,cb_busDepot.at(\cb_snap++5),
				\cb_AMD_bus,cb_busDepot.at(\cb_AMD++5),
				\cb_AMF_bus,cb_busDepot.at(\cb_AMF++5),
				\cb_EQF_bus,cb_busDepot.at(\cb_EQF++5),
				\cb_EQG_bus,cb_busDepot.at(\cb_EQG++5),
				\cb_LPfreq_bus,cb_busDepot.at(\cb_LPfreq++5),
				\cb_HPfreq_bus,cb_busDepot.at(\cb_HPfreq++5),
				\cb_filterQ_bus,cb_busDepot.at(\cb_filterQ++5),
				\cb_pan_bus,cb_busDepot.at(\cb_pan++5),
				\cb_brate_bus,cb_busDepot.at(\cb_brate++5),
				\cb_bcnt_bus,cb_busDepot.at(\cb_bcnt++5),
				\cb_SPTCH_bus,cb_busDepot.at(\cb_SPTCH++5),
				\cb_SCHNK_bus,cb_busDepot.at(\cb_SCHNK++5),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[5-1]);
			// ("triggering a thing "++(synthArray[5-1].nodeID)).postln;
		});

		rs_params = Dictionary.newFrom([
			\rs_amp,0.4,
			\rs_carHz,370,
			\rs_carAtk,0,
			\rs_carRel,0.05,
			\rs_modAmp,127,
			\rs_modHz,4000,
			\rs_rampDepth,0,
			\rs_rampDec,0,
			\rs_sdAmp,1,
			\rs_sdAtk,0,
			\rs_sdRel,0.05,
			\rs_AMD,0,
			\rs_AMF,100,
			\rs_EQF,600,
			\rs_EQG,0,
			\rs_LPfreq,24000,
			\rs_HPfreq,0,
			\rs_filterQ,1,
			\rs_pan,0,
			\rs_brate,24000,
			\rs_bcnt,24,
			\rs_SPTCH,1,
			\rs_SCHNK,1,
		]);


		rs_params.keysValuesDo({ arg key,value;
			rs_busDepot.put(key++6,Bus.control(context.server));
			rs_busDepot.at(key++6).set(value);
			this.addCommand(key, "f", { arg msg;
				// NOTE: added a little debug here
				// ["setting",key,6,msg[1],rs_busDepot.at(key++6)].postln;
				rs_busDepot.at(key++6).set(msg[1]);
			});
		});

		this.addCommand("trig_rs", "", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[6-1].notNil,{
				if (synthArray[6-1].isRunning,{
					synthArray[6-1].set(\kill_gate,-1.05);
				});
			});
			// [(\rs_amp++6),rs_busDepot.at(\rs_amp++6)].postln;
			// synthArray[6-1]=Synth.new("rs",rs_params.getPairs);
			synthArray[6-1]=Synth.new("rs",[
				\rs_amp_bus,rs_busDepot.at(\rs_amp++6),
				\rs_carHz_bus,rs_busDepot.at(\rs_carHz++6),
				\rs_carAtk_bus,rs_busDepot.at(\rs_carAtk++6),
				\rs_carRel_bus,rs_busDepot.at(\rs_carRel++6),
				\rs_modAmp_bus,rs_busDepot.at(\rs_modAmp++6),
				\rs_modHz_bus,rs_busDepot.at(\rs_modHz++6),
				\rs_rampDepth_bus,rs_busDepot.at(\rs_rampDepth++6),
				\rs_rampDec_bus,rs_busDepot.at(\rs_rampDec++6),
				\rs_sdAmp_bus,rs_busDepot.at(\rs_sdAmp++6),
				\rs_sdAtk_bus,rs_busDepot.at(\rs_sdAtk++6),
				\rs_sdRel_bus,rs_busDepot.at(\rs_sdRel++6),
				\rs_AMD_bus,rs_busDepot.at(\rs_AMD++6),
				\rs_AMF_bus,rs_busDepot.at(\rs_AMF++6),
				\rs_EQF_bus,rs_busDepot.at(\rs_EQF++6),
				\rs_EQG_bus,rs_busDepot.at(\rs_EQG++6),
				\rs_LPfreq_bus,rs_busDepot.at(\rs_LPfreq++6),
				\rs_HPfreq_bus,rs_busDepot.at(\rs_HPfreq++6),
				\rs_filterQ_bus,rs_busDepot.at(\rs_filterQ++6),
				\rs_pan_bus,rs_busDepot.at(\rs_pan++6),
				\rs_brate_bus,rs_busDepot.at(\rs_brate++6),
				\rs_bcnt_bus,rs_busDepot.at(\rs_bcnt++6),
				\rs_SPTCH_bus,rs_busDepot.at(\rs_SPTCH++6),
				\rs_SCHNK_bus,rs_busDepot.at(\rs_SCHNK++6),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[6-1]);
			// ("triggering a thing "++(synthArray[6-1].nodeID)).postln;
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
			\hh_EQF,600,
			\hh_EQG,0,
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
			hh_busDepot.put(key++7,Bus.control(context.server));
			hh_busDepot.at(key++7).set(value);
			this.addCommand(key, "f", { arg msg;
				// NOTE: added a little debug here
				// ["setting",key,7,msg[1],hh_busDepot.at(key++7)].postln;
				hh_busDepot.at(key++7).set(msg[1]);
			});
		});

		this.addCommand("trig_hh", "", { arg msg;
			// NOTE: if the synth is not nil (i.e. its been assigned),
			// then see if it is running, and if it is, then kill it
			if (synthArray[7-1].notNil,{
				if (synthArray[7-1].isRunning,{
					synthArray[7-1].set(\kill_gate,-1.05);
				});
			});
			// [(\hh_amp++7),hh_busDepot.at(\hh_amp++7)].postln;
			// synthArray[7-1]=Synth.new("hh",hh_params.getPairs);
			synthArray[7-1]=Synth.new("hh",[
				\hh_amp_bus,hh_busDepot.at(\hh_amp++7),
				\hh_carHz_bus,hh_busDepot.at(\hh_carHz++7),
				\hh_carAtk_bus,hh_busDepot.at(\hh_carAtk++7),
				\hh_carRel_bus,hh_busDepot.at(\hh_carRel++7),
				\hh_tremDepth_bus,hh_busDepot.at(\hh_tremDepth++7),
				\hh_tremHz_bus,hh_busDepot.at(\hh_tremHz++7),
				\hh_modAmp_bus,hh_busDepot.at(\hh_modAmp++7),
				\hh_modHz_bus,hh_busDepot.at(\hh_modHz++7),
				\hh_modAtk_bus,hh_busDepot.at(\hh_modAtk++7),
				\hh_modRel_bus,hh_busDepot.at(\hh_modRel++7),
				\hh_feedAmp_bus,hh_busDepot.at(\hh_feedAmp++7),
				\hh_AMD_bus,hh_busDepot.at(\hh_AMD++7),
				\hh_AMF_bus,hh_busDepot.at(\hh_AMF++7),
				\hh_EQF_bus,hh_busDepot.at(\hh_EQF++7),
				\hh_EQG_bus,hh_busDepot.at(\hh_EQG++7),
				\hh_LPfreq_bus,hh_busDepot.at(\hh_LPfreq++7),
				\hh_HPfreq_bus,hh_busDepot.at(\hh_HPfreq++7),
				\hh_filterQ_bus,hh_busDepot.at(\hh_filterQ++7),
				\hh_pan_bus,hh_busDepot.at(\hh_pan++7),
				\hh_brate_bus,hh_busDepot.at(\hh_brate++7),
				\hh_bcnt_bus,hh_busDepot.at(\hh_bcnt++7),
				\hh_SPTCH_bus,hh_busDepot.at(\hh_SPTCH++7),
				\hh_SCHNK_bus,hh_busDepot.at(\hh_SCHNK++7),
			],target:context.server); // NOTE: added the target, just in case...
			NodeWatcher.register(synthArray[7-1]);
			// ("triggering a thing "++(synthArray[7-1].nodeID)).postln;
		});

	}

	free {
		(0..9).do({arg i; synthArray[i].free});
		bd_busDepot.keysValuesDo({ arg key,value; value.free});
		sd_busDepot.keysValuesDo({ arg key,value; value.free});
		tm_busDepot.keysValuesDo({ arg key,value; value.free});
		cp_busDepot.keysValuesDo({ arg key,value; value.free});
		rs_busDepot.keysValuesDo({ arg key,value; value.free});
		cb_busDepot.keysValuesDo({ arg key,value; value.free});
		hh_busDepot.keysValuesDo({ arg key,value; value.free});
		// cy_busDepot.keysValuesDo({ arg key,value; value.free});
	}
}