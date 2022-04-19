local Kildare = {}
local specs = {}
ControlSpec = require "controlspec"
frm = require 'formatters'
local kildare_lfos = include 'lib/kildare_lfos'

local drums = {"bd","sd","tm","cp","rs","cb","hh"}

function round_form(param,quant,form)
  return(util.round(param,quant)..form)
end

function Kildare.init()

  function percent_formatter(param)
    return (param:get().."%")
  end
  
  drum_params = {
    ["bd"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 20, max = 500, warp = 'exp', default = 55, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.3, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 600, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'number', min = 0, max = 100, default = 11, formatter = function(param) return (param:get().."%") end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.3, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'SPTCH', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'SCHNK', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'AMD', name = 'amp mod depth', type = 'number', min = 0, max = 100, default = 0, formatter = function(param) return (round_form(param:get(),1,'%')) end},
      {id = 'AMF', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 8175.08, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQF', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQG', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'brate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bcnt', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'LPfreq', name = 'lo-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 19000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'HPfreq', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget}
    },
    ["sd"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 115, max = 660, warp = 'exp', default = 282.54, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.15, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 2770, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.2, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 1, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'noise'},
      {id = 'noiseAmp', name = 'noise amp', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.01, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'noiseAtk', name = 'noise attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'noiseRel', name = 'noise release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.1, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.06, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'SPTCH', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'SCHNK', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'AMD', name = 'amp mod depth', type = 'number', min = 0, max = 100, default = 0, formatter = function(param) return (round_form(param:get(),1,'%')) end},
      {id = 'AMF', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 2698.8, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQF', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 12000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQG', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'brate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bcnt', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'LPfreq', name = 'lo-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 19000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'HPfreq', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget}
    },
    ["tm"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 40, max = 200, warp = 'exp', default = 87.3, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.43, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.32, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 180, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.20, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 100, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'number', min = 0, max = 100, default = 30, formatter = function(param) return (param:get().."%") end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.06, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'SPTCH', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'SCHNK', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'AMD', name = 'amp mod depth', type = 'number', min = 0, max = 100, default = 0, formatter = function(param) return (round_form(param:get(),1,'%')) end},
      {id = 'AMF', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 2698.8, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQF', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQG', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'brate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bcnt', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'LPfreq', name = 'lo-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'HPfreq', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget}
    },
    ["cp"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 120, max = 5000, warp = 'exp', default = 1600, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.43, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 300, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.5, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 100, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'click'},
      {id = 'click', name = 'click', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'SPTCH', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'SCHNK', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'AMD', name = 'amp mod depth', type = 'number', min = 0, max = 100, default = 0, formatter = function(param) return (round_form(param:get(),1,'%')) end},
      {id = 'AMF', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 2698.8, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQF', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQG', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'brate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bcnt', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'LPfreq', name = 'lo-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'HPfreq', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget}
    },
    ["rs"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 200, max = 3600, warp = 'exp', default = 370, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 4000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {type = 'separator', name = 'snare drum'},
      {id = 'sdAmp', name = 'snare amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'sdAtk', name = 'snare attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'sdRel', name = 'snare release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'SPTCH', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'SCHNK', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'AMD', name = 'amp mod depth', type = 'number', min = 0, max = 100, default = 0, formatter = function(param) return (round_form(param:get(),1,'%')) end},
      {id = 'AMF', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 8175.08, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQF', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQG', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'brate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bcnt', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'LPfreq', name = 'lo-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 19000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'HPfreq', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget}
    },
    ["cb"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 200, max = 3600, warp = 'exp', default = 404, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.15, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'snap'},
      {id = 'snap', name = 'snap', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'number', min = 0, max = 100, default = 0, formatter = function(param) return (param:get().."%") end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 4, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'SPTCH', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'SCHNK', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'AMD', name = 'amp mod depth', type = 'number', min = 0, max = 100, default = 0, formatter = function(param) return (round_form(param:get(),1,'%')) end},
      {id = 'AMF', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 2698.8, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQF', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 12000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQG', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'brate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bcnt', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'LPfreq', name = 'lo-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 19000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'HPfreq', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget}
    },
    ["hh"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 200, max = 1400, warp = 'exp', default = 200, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.03, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 100, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form(param:get(),0.01," s")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {type = 'separator', name = 'tremolo'},
      {id = 'tremDepth', name = 'tremolo depth', type = 'number', min = 0, max = 100, default = 0, formatter = function(param) return (round_form(param:get(),1,"%")) end},
      {id = 'tremHz', name = 'tremolo rate', type = 'control', min = 0.01, max = 8000, warp = 'exp', default = 1000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'SPTCH', name = 'squish pitch', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) if param:get() == 1 then return ("off") else return (round_form(param:get(),1,'')) end end},
      {id = 'SCHNK', name = 'squish chunkiness', type = 'number', min = 1, max = 10, default = 1, formatter = function(param) return (round_form(param:get(),1,'')) end},
      {id = 'AMD', name = 'amp mod depth', type = 'number', min = 0, max = 100, default = 0, formatter = function(param) return (round_form(param:get(),1,'%')) end},
      {id = 'AMF', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 8175.08, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQF', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'EQG', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form(param:get()*100,1,"%")) end},
      {id = 'brate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round(param:get(),0.1).." hz") end},
      {id = 'bcnt', name = 'bit depth', type = 'number', min = 1, max = 24, default = 24},
      {id = 'LPfreq', name = 'lo-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 19000, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'HPfreq', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form(param:get(),0.01," hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'number', min = 0, max = 100, default = 50, formatter = function(param) return (param:get().."%") end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, formatter = frm.bipolar_as_pan_widget}
    },
  }

  params:add_separator("kildare")
  
  for j = 1,#drums do
    local k = drums[j]
    params:add_group(k, #drum_params[k])
    for i = 1, #drum_params[k] do
      local d = drum_params[k][i]
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
      elseif d.type == 'separator' then
        params:add_separator(d.name)
      end
      if d.type ~= 'separator' then
        params:set_action(k.."_"..d.id, function(x)
          engine[k.."_"..d.id](x)
        end)
      end
    end
    -- add lfos
  end

  kildare_lfos.add_params()
  
  params:bang()
  
end

return Kildare