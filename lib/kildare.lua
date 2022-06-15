local Kildare = {}
local specs = {}
ControlSpec = require 'controlspec'
frm = require 'formatters'
local kildare_lfos = include 'kildare/lib/kildare_lfos'
local musicutil = require 'musicutil'

local drums = {"bd","sd","tm","cp","rs","cb","hh"}
local fx = {"delay", "reverb", "main"}

function round_form(param,quant,form)
  return(util.round(param,quant)..form)
end

function Kildare.init(poly)

  function percent_formatter(param)
    return (param:get().."%")
  end
  
  kildare_drum_params = {
    ["bd"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'option', options = {"mono","poly"}, default = 1},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'number', min = 15, max = 67, default = 33, formatter = function(param) return (musicutil.note_num_to_name(round_form(param:get(),1,''),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get(),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.3, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 600, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'number', min = 0, max = 1, default = 0, formatter = function(param) if param:get() == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'number', min = -20, max = 20, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'number', min = -20, max = 20, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.11, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.3, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'squishPitch', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 8175.08, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget},
      {type = 'separator', name = 'fx sends'},
      {id = 'delayAmp', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'reverbAmp', name = 'reverb', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
    },
    ["sd"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'option', options = {"mono","poly"}, default = 1},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'number', min = 46, max = 76, default = 61, formatter = function(param) return (musicutil.note_num_to_name(param:get(),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get(),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.15, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 2770, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'number', min = 0, max = 1, default = 0, formatter = function(param) if param:get() == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'number', min = -20, max = 20, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'number', min = -20, max = 20, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.2, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 1, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'noise'},
      {id = 'noiseAmp', name = 'noise amp', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.01, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'noiseAtk', name = 'noise attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'noiseRel', name = 'noise release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.1, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.5, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.06, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'squishPitch', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 2698.8, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 12000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget},
      {type = 'separator', name = 'fx sends'},
      {id = 'delayAmp', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'reverbAmp', name = 'reverb', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
    },
    ["tm"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'option', options = {"mono","poly"}, default = 1},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'number', min = 27, max = 55, default = 41, formatter = function(param) return (musicutil.note_num_to_name(param:get(),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get(),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.43, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.32, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 180, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'number', min = 0, max = 1, default = 0, formatter = function(param) if param:get() == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'number', min = -20, max = 20, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'number', min = -20, max = 20, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.20, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.3, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.06, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'squishPitch', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 2698.8, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget},
      {type = 'separator', name = 'fx sends'},
      {id = 'delayAmp', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'reverbAmp', name = 'reverb', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
    },
    ["cp"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'option', options = {"mono","poly"}, default = 1},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'number', min = 47, max = 111, default = 91, formatter = function(param) return (musicutil.note_num_to_name(param:get(),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get(),0.1," semitones")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.43, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 300, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'number', min = 0, max = 1, default = 0, formatter = function(param) if param:get() == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'number', min = -20, max = 20, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'number', min = -20, max = 20, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.5, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 100, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'click'},
      {id = 'click', name = 'click', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'squishPitch', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 2698.8, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget},
      {type = 'separator', name = 'fx sends'},
      {id = 'delayAmp', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'reverbAmp', name = 'reverb', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
    },
    ["rs"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'option', options = {"mono","poly"}, default = 1},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'number', min = 55, max = 105, default = 66, formatter = function(param) return (musicutil.note_num_to_name(param:get(),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get(),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 4000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'number', min = 0, max = 1, default = 0, formatter = function(param) if param:get() == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'number', min = -20, max = 20, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'number', min = -20, max = 20, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {type = 'separator', name = 'snare drum'},
      {id = 'sdAmp', name = 'snare amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'sdAtk', name = 'snare attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'sdRel', name = 'snare release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'rampDepth', name = 'snare ramp depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'rampDec', name = 'snare ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.06, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'squishPitch', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 8175.08, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget},
      {type = 'separator', name = 'fx sends'},
      {id = 'delayAmp', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'reverbAmp', name = 'reverb', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
    },
    ["cb"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'option', options = {"mono","poly"}, default = 1},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'number', min = 55, max = 105, default = 68, formatter = function(param) return (musicutil.note_num_to_name(param:get(),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get(),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.15, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'snap'},
      {id = 'snap', name = 'snap', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 4, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'squishPitch', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 2698.8, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 12000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget},
      {type = 'separator', name = 'fx sends'},
      {id = 'delayAmp', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'reverbAmp', name = 'reverb', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
    },
    ["hh"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'option', options = {"mono","poly"}, default = 1},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'number', min = 55, max = 89, default = 55, formatter = function(param) return (musicutil.note_num_to_name(param:get(),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get(),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.03, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 100, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'number', min = 0, max = 1, default = 0, formatter = function(param) if param:get() == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'number', min = -20, max = 20, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'number', min = -20, max = 20, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'tremolo'},
      {id = 'tremDepth', name = 'tremolo depth', type = 'number', min = 0, max = 100, default = 0, formatter = function(param) return (round_form(param:get(),1,"%")) end},
      {id = 'tremHz', name = 'tremolo rate', type = 'control', min = 0.01, max = 8000, warp = 'exp', default = 1000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'squishPitch', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 8175.08, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget},
      {type = 'separator', name = 'fx sends'},
      {id = 'delayAmp', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'reverbAmp', name = 'reverb', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
    },
  }

  kildare_fx_params = {
    ["delay"] = {
      {type = 'separator', name = 'delay settings'},
      {id = 'time', name = 'time', type = 'number', min = 1, max = 128, default = 64, formatter = function (param) return param:get() end},
      {id = 'level', name = 'level', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.5, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'feedback', name = 'feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'spread', name = 'spread', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'reverbSend', name = 'send to reverb', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
    },
    ["reverb"] = {
      {type = 'separator', name = 'reverb settings'},
      {id = 'decay', name = 'decay', type = 'control', min = 0.1, max = 60, warp = 'exp', default = 2, formatter = function(param) return (util.round(param:get(),0.01).."s") end},
      {id = 'preDelay', name = 'pre delay', type = 'control', min = 0.0, max = 0.5, warp = 'lin', default = 0, formatter = function(param) return (util.round(param:get(),0.01).."s") end},
      {id = 'earlyDiff', name = 'early reflections', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 600, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modFreq', name = 'mod freq', type = 'control', min = 0.1, max = 10, warp = 'exp', default = 0.1, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modDepth', name = 'mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'level', name = 'level', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'thresh', name = 'gate threshold', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'slopeBelow', name = 'slope below', type = 'control', min = 0, max = 3, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'slopeAbove', name = 'slope above', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
    },
    ["main"] = {
      {type = 'separator', name = 'main output settings'},
      {id = 'lSHz', name = 'low shelf', type = 'control', min = 20, max = 12000, warp = 'exp', default = 600, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'lSdb', name = 'low shelf gain', type = 'number', min = -15, max = 15, default = 0, formatter = function(param) return (param:get().." dB") end},
      {id = 'lSQ', name = 'low shelf q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'hSHz', name = 'hi shelf', type = 'control', min = 800, max = 19000, warp = 'exp', default = 19000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'hSdb', name = 'hi shelf gain', type = 'number', min = -15, max = 15, default = 0, formatter = function(param) return (param:get().." dB") end},
      {id = 'hSQ', name = 'hi shelf q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'eqHz', name = 'eq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 6000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'eqdb', name = 'eq gain', type = 'number', min = -30, max = 15, default = 0, formatter = function(param) return (param:get().." dB") end},
      {id = 'eqQ', name = 'eq q', type = 'number', min = -100, max = 100, default = 0, formatter = function(param) return (param:get().."%") end},

    }
  }

  params:add_separator("kildare")

  if engine.name ~= "Kildare" then
    params:add_option("no_kildare","----- kildare not loaded -----",{" "})
  end
  
  for j = 1,#drums do
    local k = drums[j]
    params:add_group(k, #kildare_drum_params[k])
    for i = 1, #kildare_drum_params[k] do
      local d = kildare_drum_params[k][i]
      if d.type == 'control' then
        params:add_control(
          k.."_"..d.id,
          d.name,
          ControlSpec.new(d.min, d.max, d.warp, 0, d.default),
          d.formatter
        )
      elseif d.type == 'number' then
        params:add_number(
          k.."_"..d.id,
          d.name,
          d.min,
          d.max,
          d.default,
          d.formatter
        )
      elseif d.type == "option" then
        params:add_option(
          k.."_"..d.id,
          d.name,
          d.options,
          d.default
        )
      elseif d.type == 'separator' then
        params:add_separator(d.name)
      end
      if d.type ~= 'separator' then
        if d.id ~= "carHz" and d.id ~= "poly" then
          params:set_action(k.."_"..d.id, function(x)
            if engine.name == "Kildare" then
              engine.set_param(k, d.id, x)
            end
          end)
        elseif d.id == "carHz" then
          params:set_action(k.."_"..d.id, function(x)
            if engine.name == "Kildare" then
              engine.set_param(k, d.id, musicutil.note_num_to_freq(x))
            end
          end)
        elseif d.id == "poly" then
          params:set_action(k.."_"..d.id, function(x)
            if engine.name == "Kildare" then
              engine.set_param(k, d.id, x == 1 and 0 or 1)
            end
          end)
          if not poly then
            params:hide(k.."_"..d.id) -- avoid exposing poly for performance management
          end
        end
      end
    end
  end

  for j = 1,#fx do
    local k = fx[j]
    params:add_group(k, #kildare_fx_params[k])
    for i = 1, #kildare_fx_params[k] do
      local d = kildare_fx_params[k][i]
      if d.type == 'control' then
        params:add_control(
          k.."_"..d.id,
          d.name,
          ControlSpec.new(d.min, d.max, d.warp, 0, d.default),
          d.formatter
        )
      elseif d.type == 'number' then
        params:add_number(
          k.."_"..d.id,
          d.name,
          d.min,
          d.max,
          d.default,
          d.formatter
        )
      elseif d.type == "option" then
        params:add_option(
          k.."_"..d.id,
          d.name,
          d.options,
          d.default
        )
      elseif d.type == 'separator' then
        params:add_separator(d.name)
      end
      if d.type ~= 'separator' then
        params:set_action(k.."_"..d.id, function(x)
          if engine.name == "Kildare" then
            if k == "delay" and d.id == "time" then
              engine["set_"..k.."_param"](d.id, clock.get_beat_sec() * x/128)
            else
              engine["set_"..k.."_param"](d.id, x)
            end
          end
        end)
      end
    end
  end

  kildare_lfos.add_params(poly)
  
  params:bang()
  
end

return Kildare