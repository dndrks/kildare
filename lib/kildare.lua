local Kildare = {}
local specs = {}
local ControlSpec = require 'controlspec'
local frm = require 'formatters'
Kildare.lfos = include 'kildare/lib/kildare_lfos'
local musicutil = require 'musicutil'

Kildare.drums = {"bd","sd","tm","cp","rs","cb","hh","sample1","sample2","sample3"}
local swappable_drums = {'bd','sd','tm','cp','rs','cb','hh','saw','fld'}
Kildare.fx = {"delay", "feedback", "main"}
local fx = {"delay", "feedback", "main"}

Kildare.soundfile_append = ''

local sox_installed = os.execute('which sox')

function round_form(param,quant,form)
  return(util.round(param,quant)..form)
end

function bipolar_as_pan_widget(param)
  local dots_per_side = 10
  local widget
  local function add_dots(num_dots)
    for i=1,num_dots do widget = (widget or "").."." end
  end
  local function add_bar()
    widget = (widget or "").."|"
  end
  local function format(param, value, units)
    return value.." "..(units or param.controlspec.units or "")
  end

  local value = type(param) == 'table' and param:get() or param
  local pan_side = math.abs(value)
  local pan_side_percentage = util.round(pan_side*100)
  local descr
  local dots_left
  local dots_right

  if value > 0 then
    dots_left = dots_per_side+util.round(pan_side*dots_per_side)
    dots_right = util.round((1-pan_side)*dots_per_side)
    if pan_side_percentage >= 1 then
      descr = "R"..pan_side_percentage
    end
  elseif value < 0 then
    dots_left = util.round((1-pan_side)*dots_per_side)
    dots_right = dots_per_side+util.round(pan_side*dots_per_side)
    if pan_side_percentage >= 1 then
     descr = "L"..pan_side_percentage
    end
  else
    dots_left = dots_per_side
    dots_right = dots_per_side
  end

  if descr == nil then
    descr = "MID"
  end

  add_bar()
  add_dots(dots_left)
  add_bar()
  add_dots(dots_right)
  add_bar()

  return format(param, descr.." "..widget, "")
end

function Kildare.folder_callback()
end

function Kildare.file_callback()
end

function Kildare.clear_callback()
end

function Kildare.voice_param_callback()
end

function Kildare.model_change_callback()
end

function Kildare.move_audio_into_perm(new_folder)
  local parent_folder = _path.audio..'kildare/TEMP/'
  if util.file_exists(parent_folder) then
    if not util.file_exists(new_folder) then
      os.execute('mkdir -p '..new_folder)
    end
    for k,v in pairs(util.scandir(parent_folder)) do
      os.execute('cp -R '..parent_folder..v..' '..new_folder)
      for i = 1,3 do
        local split_at = string.match(params:get('sample'..i..'_sampleFile'), "^.*()/")
        local folder = string.sub(params:get('sample'..i..'_sampleFile'), 1, split_at)
        if folder == (parent_folder..v) then
          print('sample'..i..' is assigned to '..folder..', reassigning to '..new_folder..v)
          params:set('sample'..i..'_sampleFile', new_folder..v..util.scandir(new_folder..v)[1])
        end
      end
      os.execute('rm -r '..parent_folder..v)
    end
  end
end

function Kildare.purge_saved_audio()
  local parent_folder = _path.audio..'kildare/TEMP/'
  if util.file_exists(parent_folder) then
    for k,v in pairs(util.scandir(parent_folder)) do
      os.execute('rm -r '..parent_folder..v)
    end
  end
end

function Kildare.rebuild_model_params(i,current_model)
  for j = 1,#swappable_drums do
    if swappable_drums[j] ~= current_model then
      -- local swappable_drums_iter = 1
      for k,v in pairs(kildare_drum_params[swappable_drums[j]]) do
        if v.type == 'separator' then
          params:hide(i..'_separator_'..swappable_drums[j]..'_'..v.name)
        else
          params:hide(i..'_'..swappable_drums[j]..'_'..v.id)
        end
      end
    else
      for k,v in pairs(kildare_drum_params[swappable_drums[j]]) do
        if v.type == 'separator' then
          params:show(i..'_separator_'..swappable_drums[j]..'_'..v.name)
        else
          params:show(i..'_'..swappable_drums[j]..'_'..v.id)
        end
      end
    end
  end
  params.params[params.lookup['kildare_'..i..'_group']].name = i..': '..current_model
  _menu.rebuild_params()
  Kildare.model_change_callback(i,current_model)
  Kildare.push_new_model_params(i,current_model)
  Kildare.lfos.build_params_static(true)
  if Kildare.loaded then
    Kildare.lfos.rebuild_model_spec(i,true)
    for j = 1,Kildare.lfos.count do
      if params:get('lfo_target_track_'..j) == i then
        Kildare.lfos.change_target(j)
      end
    end
  end
end

function Kildare.push_new_model_params(i,current_model)
  for j = 1,#swappable_drums do
    if swappable_drums[j] == current_model then
      for k,v in pairs(kildare_drum_params[swappable_drums[j]]) do
        if v.type ~= 'separator' then
          params.params[params.lookup[i..'_'..swappable_drums[j]..'_'..v.id]]:bang()
        end
      end
    end
  end
end

function Kildare.push_model_to_lfos(i,current_model)

end

function Kildare.init(poly)

  local extensions = _path.home .. '/.local/share/SuperCollider/Extensions/'
  if not util.file_exists(extensions..'miSCellaneous_lib/Classes/WaveFolding/') then
    util.os_capture('mkdir -p '..extensions ..'miSCellaneous_lib/Classes/WaveFolding/')
  end
  local install_these = {'SmoothClip.sc', 'SmoothFold.sc'}
  for i = 1,#install_these do
    if not util.file_exists(extensions..'miSCellaneous_lib/Classes/WaveFolding/'..install_these[i]) then
      util.os_capture('mkdir -p '..extensions ..'miSCellaneous_lib/Classes/WaveFolding/')
      util.os_capture('cp ' .. _path.code .. 'kildare/lib/ignore/' .. install_these[i].." " .. extensions..'miSCellaneous_lib/Classes/WaveFolding/'..install_these[i])
      print("installing "..install_these[i])
    end
  end



  function percent_formatter(param)
    return ((type(param) == 'table' and param:get() or param).."%")
  end

  local sample_params = {
    {type = 'separator', name = 'sample management'},
    {lfo_exclude = true, type = 'option', id = 'sampleMode', name = 'play mode', options = {"chop", "playthrough", "distribute"}, default = 1},
    {lfo_exclude = true, type = 'file', id = 'sampleFile', name = 'load', default = _path.audio},
    {lfo_exclude = true, type = 'binary', id = 'sampleClear', name = 'clear', behavior = 'trigger'},
    {type = 'separator', name = 'voice params'},
    {id = 'poly', name = 'polyphony', type = 'control', min = 1, max = 2,  step = 1, warp = "lin", default = 1, quantum = 1, formatter = function(param) local modes = {"mono","poly"} return modes[(type(param) == 'table' and param:get() or param)] end},
    {id = 'amp', name = 'amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, quantum = 1/125, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    {id = 'sampleEnv', name = 'amp envelope', type = 'control', min = 0, max = 1, warp = "lin", default = 0, quantum = 1, step = 1, formatter = function(param) local modes = {"off","on"} return modes[(type(param) == 'table' and param:get() or param)+1] end},
    {id = 'sampleAtk', name = 'amp attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
    {id = 'sampleRel', name = 'amp release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
    {id = 'playbackRateBase', name = 'rate', type = 'control', min = 1, max = 11, warp = 'lin', default = 9, step = 1, quantum = 1/10, formatter = function(param) local rate_options = {-4, -2, -1, -0.5, -0.25, 0, 0.25, 0.5, 1, 2, 4} return rate_options[(type(param) == 'table' and param:get() or param)]..'x' end},
    {id = 'playbackRateOffset', name = 'offset', type = 'control', min = -24, max = 24, warp = 'lin', default = 0, step = 1, quantum = 1/48, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1," semitones")) end},
    {id = 'playbackPitchControl', name = 'pitch control', type = 'control', min = -12, max = 12, warp = 'lin', default = 0, step = 1/10, quantum = 1/240, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01,"%")) end},
    {id = 'loop', name = 'loop', type = 'control', min = 0, max = 1, warp = "lin", default = 0, quantum = 1, formatter = function(param) local modes = {"off","on"} return modes[(type(param) == 'table' and param:get() or param)+1] end},
    {type = 'separator', name = 'additional processing'},
    {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 8175.08, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
    {id = 'squishPitch', name = 'squish pitch', type = 'control', min = 1, max = 10, warp = 'lin', default = 1, quantum = 1/9, step = 1, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 1 then return ("off") else return (round_form((type(param) == 'table' and param:get() or param),1,'')) end end},
    {id = 'squishChunk', name = 'squish chunkiness', type = 'control', min = 1, max = 10, warp = 'lin', default = 1, quantum = 1/9, step = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
    {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round((type(param) == 'table' and param:get() or param),0.1).." Hz") end},
    {id = 'bitCount', name = 'bit depth', type = 'control', min = 1, max = 24, warp = 'lin', default = 24, quantum = 1/23, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1," bit")) end},
    {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
    {id = 'eqAmp', name = 'eq gain', type = 'control', min = -2, max = 2, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
    {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
    {id = 'filterQ', name = 'filter q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
    {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, quantum = 1/200, formatter = function(param) return (bipolar_as_pan_widget(type(param) == 'table' and param:get() or param)) end},
    {type = 'separator', name = 'fx sends'},
    {id = 'delaySend', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    {id = 'delayEnv', name = 'delay envelope', type = 'control', min = 0, max = 1, warp = "lin", default = 0,  step = 1, quantum = 1, formatter = function(param) local modes = {"off","on"} return modes[(type(param) == 'table' and param:get() or param)+1] end},
    {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
    {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
    {id = 'feedbackSend', name = 'feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0,  formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
  }
  
  kildare_drum_params = {
    ["bd"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'control', min = 1, max = 2, warp = "lin", default = 1, quantum = 1,  step = 1, formatter = function(param) local modes = {"mono","poly"} return modes[(type(param) == 'table' and param:get() or param)] end},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, quantum = 1/125, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 15, max = 67, warp = 'lin', default = 33, step = 1, formatter = function(param) return (musicutil.note_num_to_name((type(param) == 'table' and param:get() or param),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin', default = 0,  step = 1/10, quantum = 1/240, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.3, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carCurve', name = 'carrier env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 600, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'control', warp = 'lin', min = 0, max = 1, default = 0,  step = 1, quantum = 1, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'control', min = -20, max = 20, warp = 'lin', default = 1,  step = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'control', min = -20, max = 20, warp = 'lin', default = 1,  step = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'modCurve', name = 'modulator env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.11, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.3, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 8175.08, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'foldLo', name = 'wavefold (lo)', type = 'control', min = -1, max = 1, warp = 'lin',  default = -1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'foldHi', name = 'wavefold (hi)', type = 'control', min = -1, max = 1, warp = 'lin',  default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'foldRange', name = 'wavefold range', type = 'control', min = 0, max = 1, warp = 'lin',  default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'foldSmooth', name = 'wavefold smoothing', type = 'control', min = 0, max = 1, warp = 'lin',  default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'foldAmount', name = 'wavefold amount', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.5, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'squishPitch', name = 'squish pitch', type = 'control', min = 1, max = 10, warp = 'lin',  step = 1, default = 1, quantum = 1/9, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 1 then return ("off") else return (round_form((type(param) == 'table' and param:get() or param),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'control', min = 1, max = 10, warp = 'lin',  step = 1, default = 1, quantum = 1/9, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round((type(param) == 'table' and param:get() or param),0.1).." Hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'control', min = 1, max = 24, warp = 'lin', default = 24, quantum = 1/23, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1," bit")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = -2, max = 2, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpCurve', name = 'lo-pass env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, quantum = 1/200, formatter = function(param) return (bipolar_as_pan_widget(type(param) == 'table' and param or param)) end},
      {type = 'separator', name = 'fx sends'},
      {id = 'delaySend', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'delayEnv', name = 'delay envelope', type = 'control', min = 0, max = 1, warp = "lin", default = 0,  step = 1, quantum = 1, formatter = function(param) local modes = {"off","on"} return modes[(type(param) == 'table' and param:get() or param)+1] end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'feedbackSend', name = 'feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    },
    ["sd"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'control', min = 1, max = 2, warp = "lin", default = 1, quantum = 1,  step = 1, formatter = function(param) local modes = {"mono","poly"} return modes[(type(param) == 'table' and param:get() or param)] end},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.6, quantum = 1/125, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 46, max = 76, default = 61, warp = 'lin',  step = 1, formatter = function(param) return (musicutil.note_num_to_name((type(param) == 'table' and param:get() or param),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin',  step = 1/10, default = 0, quantum = 1/240, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.15, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carCurve', name = 'carrier env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 2770, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'control', warp = 'lin', min = 0, max = 1,  step = 1, quantum = 1, default = 0, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'control', min = -20, max = 20, warp = 'lin',  step = 1, default = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'control', min = -20, max = 20, warp = 'lin',  step = 1, default = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'modCurve', name = 'modulator env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {type = 'separator', name = 'noise'},
      {id = 'noiseAmp', name = 'noise amp', type = 'control', min = 0, max = 1, warp = 'lin',  step = 0.01, default = 0.01, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'noiseAtk', name = 'noise attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'noiseRel', name = 'noise release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'noiseCurve', name = 'noise env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.5, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.06, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 2698.8, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'squishPitch', name = 'squish pitch', type = 'control', min = 1, max = 10, warp = 'lin',  step = 1, default = 1, quantum = 1/9, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 1 then return ("off") else return (round_form((type(param) == 'table' and param:get() or param),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'control', min = 1, max = 10, warp = 'lin',  step = 1, default = 1, quantum = 1/9, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round((type(param) == 'table' and param:get() or param),0.1).." Hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'control', min = 1, max = 24, warp = 'lin', default = 24, quantum = 1/23, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1," bit")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 12000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpCurve', name = 'lo-pass env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, quantum = 1/200, formatter = function(param) return (bipolar_as_pan_widget(type(param) == 'table' and param or param)) end},
      {type = 'separator', name = 'fx sends'},
      {id = 'delaySend', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'delayEnv', name = 'delay envelope', type = 'control', min = 0, max = 1, warp = "lin", default = 0,  step = 1, quantum = 1, formatter = function(param) local modes = {"off","on"} return modes[(type(param) == 'table' and param:get() or param)+1] end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'feedbackSend', name = 'feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    },
    ["tm"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'control', min = 1, max = 2, warp = "lin", default = 1,  step = 1, quantum = 1, formatter = function(param) local modes = {"mono","poly"} return modes[(type(param) == 'table' and param:get() or param)] end},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.6, quantum = 1/125, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 27, max = 55, default = 41,  step = 1, warp = 'lin', formatter = function(param) return (musicutil.note_num_to_name((type(param) == 'table' and param:get() or param),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin',  step = 1/10, default = 0, quantum = 1/240, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.43, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carCurve', name = 'carrier env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.32, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 180, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'control', warp = 'lin', min = 0, max = 1,  step = 1, quantum = 1, default = 0, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'control', min = -20, max = 20,  step = 1, warp = 'lin', default = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'control', min = -20, max = 20,  step = 1, warp = 'lin', default = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.20, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'modCurve', name = 'modulator env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.3, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.06, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 2698.8, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'squishPitch', name = 'squish pitch', type = 'control', min = 1, max = 10,  step = 1, warp = 'lin', default = 1, quantum = 1/9, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 1 then return ("off") else return (round_form((type(param) == 'table' and param:get() or param),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'control', min = 1, max = 10,  step = 1, warp = 'lin', default = 1, quantum = 1/9, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round((type(param) == 'table' and param:get() or param),0.1).." Hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'control', min = 1, max = 24, warp = 'lin', default = 24, quantum = 1/23, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1," bit")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpCurve', name = 'lo-pass env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, quantum = 1/200, formatter = function(param) return (bipolar_as_pan_widget(type(param) == 'table' and param or param)) end},
      {type = 'separator', name = 'fx sends'},
      {id = 'delaySend', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'delayEnv', name = 'delay envelope', type = 'control', min = 0, max = 1, warp = "lin", default = 0,  step = 1, quantum = 1, formatter = function(param) local modes = {"off","on"} return modes[(type(param) == 'table' and param:get() or param)+1] end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'feedbackSend', name = 'feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    },
    ["cp"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'control', min = 1, max = 2, warp = "lin", default = 1,  step = 1, quantum = 1, formatter = function(param) local modes = {"mono","poly"} return modes[(type(param) == 'table' and param:get() or param)] end},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.6, quantum = 1/125, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 47, max = 111, default = 91,  step = 1, warp = 'lin', formatter = function(param) return (musicutil.note_num_to_name((type(param) == 'table' and param:get() or param),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin',  step = 1/10, default = 0, quantum = 1/240, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.1," semitones")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.43, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 300, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'control', warp = 'lin', min = 0, max = 1, step = 1, quantum = 1, default = 0, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'control', min = -20, max = 20,  step = 1, warp = 'lin', default = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'control', min = -20, max = 20,  step = 1, warp = 'lin', default = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.5, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {type = 'separator', name = 'click'},
      {id = 'click', name = 'click', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 2698.8, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'squishPitch', name = 'squish pitch', type = 'control', min = 1, max = 10,  step = 1, warp = 'lin', default = 1, quantum = 1/9, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 1 then return ("off") else return (round_form((type(param) == 'table' and param:get() or param),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'control', min = 1, max = 10,  step = 1, warp = 'lin', default = 1, quantum = 1/9, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round((type(param) == 'table' and param:get() or param),0.1).." Hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'control', min = 1, max = 24, warp = 'lin', default = 24, quantum = 1/23, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1," bit")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpCurve', name = 'lo-pass env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, quantum = 1/200, formatter = function(param) return (bipolar_as_pan_widget(type(param) == 'table' and param or param)) end},
      {type = 'separator', name = 'fx sends'},
      {id = 'delaySend', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'delayEnv', name = 'delay envelope', type = 'control', min = 0, max = 1, warp = "lin", default = 0,  step = 1, quantum = 1, formatter = function(param) local modes = {"off","on"} return modes[(type(param) == 'table' and param:get() or param)+1] end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'feedbackSend', name = 'feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    },
    ["rs"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'control', min = 1, max = 2, warp = "lin",  step = 1, default = 1, quantum = 1, formatter = function(param) local modes = {"mono","poly"} return modes[(type(param) == 'table' and param:get() or param)] end},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 1.0, quantum = 1/125, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 55, max = 105, default = 66,  step = 1, warp = 'lin', formatter = function(param) return (musicutil.note_num_to_name((type(param) == 'table' and param:get() or param),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin',  step = 1/10, default = 0, quantum = 1/240, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carCurve', name = 'carrier env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 4000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'control', warp = 'lin', min = 0, max = 1,  step = 1, quantum = 1, default = 0, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'control', min = -20, max = 20,  step = 1, warp = 'lin', default = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'control', min = -20, max = 20,  step = 1, warp = 'lin', default = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {type = 'separator', name = 'snare drum'},
      {id = 'sdAmp', name = 'snare amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'sdAtk', name = 'snare attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'sdRel', name = 'snare release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'sdCurve', name = 'snare env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'rampDepth', name = 'snare ramp depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'rampDec', name = 'snare ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.06, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 8175.08, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'squishPitch', name = 'squish pitch', type = 'control', min = 1, max = 10,  step = 1, warp = 'lin', default = 1, quantum = 1/9, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 1 then return ("off") else return (round_form((type(param) == 'table' and param:get() or param),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'control', min = 1, max = 10,  step = 1, warp = 'lin', default = 1, quantum = 1/9, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round((type(param) == 'table' and param:get() or param),0.1).." Hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'control', min = 1, max = 24, warp = 'lin', default = 24, quantum = 1/23, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1," bit")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpCurve', name = 'lo-pass env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, quantum = 1/200, formatter = function(param) return (bipolar_as_pan_widget(type(param) == 'table' and param or param)) end},
      {type = 'separator', name = 'fx sends'},
      {id = 'delaySend', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'delayEnv', name = 'delay envelope', type = 'control', min = 0, max = 1, warp = "lin", default = 0,  step = 1, quantum = 1, formatter = function(param) local modes = {"off","on"} return modes[(type(param) == 'table' and param:get() or param)+1] end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'feedbackSend', name = 'feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    },
    ["cb"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'control', min = 1, max = 2, warp = "lin",  step = 1, default = 1, quantum = 1, formatter = function(param) local modes = {"mono","poly"} return modes[(type(param) == 'table' and param:get() or param)] end},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.8, quantum = 1/125, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 55, max = 105,  step = 1, default = 68, warp = 'lin', formatter = function(param) return (musicutil.note_num_to_name((type(param) == 'table' and param:get() or param),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12,  step = 1/10, warp = 'lin', default = 0, quantum = 1/240, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.15, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carCurve', name = 'carrier env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'modCurve', name = 'modulator env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {type = 'separator', name = 'snap'},
      {id = 'snap', name = 'snap', type = 'control', min = 0, max = 1, warp = 'lin', step = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 4, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 2698.8, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'squishPitch', name = 'squish pitch', type = 'control', min = 1, max = 10,  step = 1, warp = 'lin', default = 1, quantum = 1/9, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 1 then return ("off") else return (round_form((type(param) == 'table' and param:get() or param),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'control', min = 1, max = 10,  step = 1, warp = 'lin', default = 1, quantum = 1/9, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round((type(param) == 'table' and param:get() or param),0.1).." Hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'control', min = 1, max = 24, warp = 'lin', default = 24, quantum = 1/23, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1," bit")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 12000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpCurve', name = 'lo-pass env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, quantum = 1/200, formatter = function(param) return (bipolar_as_pan_widget(type(param) == 'table' and param or param)) end},
      {type = 'separator', name = 'fx sends'},
      {id = 'delaySend', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'delayEnv', name = 'delay envelope', type = 'control', min = 0, max = 1, warp = "lin", default = 0,  step = 1, quantum = 1, formatter = function(param) local modes = {"off","on"} return modes[(type(param) == 'table' and param:get() or param)+1] end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'feedbackSend', name = 'feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    },
    ["hh"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'control', min = 1, max = 2, warp = "lin",  step = 1, default = 1, quantum = 1, formatter = function(param) local modes = {"mono","poly"} return modes[(type(param) == 'table' and param:get() or param)] end},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.6, quantum = 1/125, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 55, max = 89, default = 55,  step = 1, warp = 'lin', formatter = function(param) return (musicutil.note_num_to_name((type(param) == 'table' and param:get() or param),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin',  step = 1/10, default = 0, quantum = 1/240, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.03, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carCurve', name = 'carrier env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'control', warp = 'lin', min = 0, max = 1, step = 1, quantum = 1, default = 0, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'control', min = -20, max = 20, warp = 'lin', step = 1, default = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'control', min = -20, max = 20, warp = 'lin', step = 1, default = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'modCurve', name = 'modulator env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {type = 'separator', name = 'tremolo'},
      {id = 'tremDepth', name = 'tremolo depth', type = 'number', min = 0, max = 100, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'tremHz', name = 'tremolo rate', type = 'control', min = 0.01, max = 8000, warp = 'exp', default = 1000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 8175.08, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'squishPitch', name = 'squish pitch', type = 'control', min = 1, max = 10, warp = 'lin',  step = 1, default = 1, quantum = 1/9, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 1 then return ("off") else return (round_form((type(param) == 'table' and param:get() or param),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'control', min = 1, max = 10, warp = 'lin',  step = 1, default = 1, quantum = 1/9, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round((type(param) == 'table' and param:get() or param),0.1).." Hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'control', min = 1, max = 24, warp = 'lin', default = 24, quantum = 1/23, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1," bit")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpCurve', name = 'lo-pass env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, quantum = 1/200, formatter = function(param) return (bipolar_as_pan_widget(type(param) == 'table' and param or param)) end},
      {type = 'separator', name = 'fx sends'},
      {id = 'delaySend', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'delayEnv', name = 'delay envelope', type = 'control', min = 0, max = 1, warp = "lin", default = 0,  step = 1, quantum = 1, formatter = function(param) local modes = {"off","on"} return modes[(type(param) == 'table' and param:get() or param)+1] end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'feedbackSend', name = 'feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    },
    ["saw"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'control', min = 1, max = 2, warp = "lin", default = 1, quantum = 1,  step = 1, formatter = function(param) local modes = {"mono","poly"} return modes[(type(param) == 'table' and param:get() or param)] end},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, quantum = 1/125, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 15, max = 67, warp = 'lin', default = 33, step = 1, formatter = function(param) return (musicutil.note_num_to_name((type(param) == 'table' and param:get() or param),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin', default = 0,  step = 1/10, quantum = 1/240, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.3, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carCurve', name = 'carrier env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'phaseOff1', name = 'phase offset 1', type = 'control', min = 0, max = 1, warp = 'lin', default = 2/3, quantum = 0.01, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'phaseOff2', name = 'phase offset 2', type = 'control', min = 1, max = 2, warp = 'lin', default = 4/3, quantum = 0.01, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'subSqAmp', name = 'sub amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 1, quantum = 1/125, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'subSqPW', name = 'sub pw', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.5, quantum = 0.01, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'subSqPWMRate', name = 'sub pwm rate', type = 'control', min = 0.001, max = 30, warp = 'exp', default = 0.03, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'subSqPWMAmt', name = 'sub pwm amount', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, quantum = 0.01, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 600, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'control', warp = 'lin', min = 0, max = 1, default = 0,  step = 1, quantum = 1, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'control', min = -20, max = 20, warp = 'lin', default = 1,  step = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'control', min = -20, max = 20, warp = 'lin', default = 1,  step = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'modCurve', name = 'modulator env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.3, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 8175.08, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'squishPitch', name = 'squish pitch', type = 'control', min = 1, max = 10, warp = 'lin',  step = 1, default = 1, quantum = 1/9, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 1 then return ("off") else return (round_form((type(param) == 'table' and param:get() or param),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'control', min = 1, max = 10, warp = 'lin',  step = 1, default = 1, quantum = 1/9, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round((type(param) == 'table' and param:get() or param),0.1).." Hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'control', min = 1, max = 24, warp = 'lin', default = 24, quantum = 1/23, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1," bit")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = -2, max = 2, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpCurve', name = 'lo-pass env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 10, max = 24000, warp = 'exp', default = 10, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, quantum = 1/200, formatter = function(param) return (bipolar_as_pan_widget(type(param) == 'table' and param or param)) end},
      {type = 'separator', name = 'fx sends'},
      {id = 'delaySend', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'delayEnv', name = 'delay envelope', type = 'control', min = 0, max = 1, warp = "lin", default = 0,  step = 1, quantum = 1, formatter = function(param) local modes = {"off","on"} return modes[(type(param) == 'table' and param:get() or param)+1] end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'feedbackSend', name = 'feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    },
    ["fld"] = {
      {type = 'separator', name = 'carrier'},
      {id = 'poly', name = 'polyphony', type = 'control', min = 1, max = 2, warp = "lin", default = 1, quantum = 1,  step = 1, formatter = function(param) local modes = {"mono","poly"} return modes[(type(param) == 'table' and param:get() or param)] end},
      {id = 'amp', name = 'carrier amp', type = 'control', min = 0, max = 1.25, warp = 'lin', default = 0.7, quantum = 1/125, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'carHz', name = 'carrier freq', type = 'control', min = 0, max = 127, warp = 'lin', default = 60, step = 1, formatter = function(param) return (musicutil.note_num_to_name((type(param) == 'table' and param:get() or param),true)) end},
      {id = 'carDetune', name = 'detune', type = 'control', min = -12, max = 12, warp = 'lin', default = 0,  step = 1/10, quantum = 1/240, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.1," semitones")) end},
      {id = 'carAtk', name = 'carrier attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carRel', name = 'carrier release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 3.3, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'carCurve', name = 'carrier env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {type = 'separator', name = 'modulator'},
      {id = 'modAmp', name = 'modulator presence', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'modHz', name = 'modulator freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 600, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'modFollow', name = '--> freq from carrier?', type = 'control', warp = 'lin', min = 0, max = 1, default = 0,  step = 1, quantum = 1, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 0 then return ("no") else return ("yes") end end},
      {id = 'modNum', name = '--> modulator num', type = 'control', min = -20, max = 20, warp = 'lin', default = 1,  step = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modDenum', name = '--> modulator denum', type = 'control', min = -20, max = 20, warp = 'lin', default = 1,  step = 1, quantum = 1/40, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'modAtk', name = 'modulator attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'modRel', name = 'modulator release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 3.3, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'modCurve', name = 'modulator env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'feedAmp', name = 'modulator feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {type = 'separator', name = 'pitch ramp'},
      {id = 'rampDepth', name = 'ramp depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'rampDec', name = 'ramp decay', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.3, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'amDepth', name = 'amp mod depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'amHz', name = 'amp mod freq', type = 'control', min = 0.001, max = 12000, warp = 'exp', default = 8175.08, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'foldLo', name = 'wavefold (lo)', type = 'control', min = -1, max = 1, warp = 'lin',  default = -1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'foldHi', name = 'wavefold (hi)', type = 'control', min = -1, max = 1, warp = 'lin',  default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'foldRange', name = 'wavefold range', type = 'control', min = 0, max = 1, warp = 'lin',  default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'foldSmooth', name = 'wavefold smoothing', type = 'control', min = 0, max = 1, warp = 'lin',  default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'foldAmount', name = 'wavefold amount', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.5, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'squishPitch', name = 'squish pitch', type = 'control', min = 1, max = 10, warp = 'lin',  step = 1, default = 1, quantum = 1/9, formatter = function(param) if (type(param) == 'table' and param:get() or param) == 1 then return ("off") else return (round_form((type(param) == 'table' and param:get() or param),1,'')) end end},
      {id = 'squishChunk', name = 'squish chunkiness', type = 'control', min = 1, max = 10, warp = 'lin',  step = 1, default = 1, quantum = 1/9, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,'')) end},
      {id = 'bitRate', name = 'bit rate', type = 'control', min = 20, max = 24000, warp = 'exp', default = 24000, formatter = function(param) return (util.round((type(param) == 'table' and param:get() or param),0.1).." Hz") end},
      {id = 'bitCount', name = 'bit depth', type = 'control', min = 1, max = 24, warp = 'lin', default = 24, quantum = 1/23, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1," bit")) end},
      {id = 'eqHz', name = 'eq freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 6000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'eqAmp', name = 'eq gain', type = 'control', min = -2, max = 2, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'lpAtk', name = 'lo-pass attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpRel', name = 'lo-pass release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.05, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'lpCurve', name = 'lo-pass env curve', type = 'control', min = -12, max = 4, warp = 'lin', default = -4, quantum = 1/160, formatter = function(param) return (round_form(
        util.linlin(-12,4,0,100,(type(param) == 'table' and param:get() or param)),
        1,"%")) end},
      {id = 'lpDepth', name = 'lo-pass env depth', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, quantum = 1/200, formatter = function(param) return (bipolar_as_pan_widget(type(param) == 'table' and param or param)) end},
      {type = 'separator', name = 'fx sends'},
      {id = 'delaySend', name = 'delay', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'delayEnv', name = 'delay envelope', type = 'control', min = 0, max = 1, warp = "lin", default = 0,  step = 1, quantum = 1, formatter = function(param) local modes = {"off","on"} return modes[(type(param) == 'table' and param:get() or param)+1] end},
      {id = 'delayAtk', name = 'delay send attack', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 0.001, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'delayRel', name = 'delay send release', type = 'control', min = 0.001, max = 10, warp = 'exp', default = 2, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," s")) end},
      {id = 'feedbackSend', name = 'feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    },
    ["sample1"] = sample_params,
    ["sample2"] = sample_params,
    ["sample3"] = sample_params
  }

  local feedback_channels = {'a','b','c'}

  kildare_fx_params = {
    ["delay"] = {
      {type = 'separator', name = 'delay settings'},
      {id = 'time', name = 'time', type = 'control', min = 1, max = 128, warp = 'lin', default = 64, quantum = 1/127, formatter = function (param) return round_form((type(param) == 'table' and param:get() or param),1,"/128") end},
      {id = 'level', name = 'level', type = 'control', min = 0, max = 2, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'feedback', name = 'feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0.7, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'spread', name = 'spread', type = 'control', min = 0, max = 1, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'pan', name = 'pan', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, quantum = 1/200, formatter = function(param) return (bipolar_as_pan_widget(type(param) == 'table' and param or param)) end},
      {type = 'separator', name = 'additional processing'},
      {id = 'lpHz', name = 'lo-pass freq', type = 'control', min = 20, max = 20000, warp = 'exp', default = 20000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'hpHz', name = 'hi-pass freq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 20, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'filterQ', name = 'filter q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'feedbackSend', name = 'send to feedback', type = 'control', min = 0, max = 1, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    },
    ["feedback"] = {
      {type = 'separator', name = 'mix'},
      {id = 'mainMixer_mixLevel', name = 'main output level', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'mainMixer_mixSpread', name = 'stereo spread', type = 'control', min = 0, max = 1, warp = 'lin', step = 0.01, quantum = 0.01, default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'mainMixer_mixCenter', name = 'stereo center', type = 'control', min = -1, max = 1, warp = 'lin', default = 0, quantum = 1/200, formatter = function(param) return (bipolar_as_pan_widget(type(param) == 'table' and param or param)) end},
      {type = 'separator', name = 'A'},
      {id = 'aMixer_inAmp', name = 'A <- engine', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'mainMixer_inA', name = 'A -> mixer', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'aMixer_inA', name = 'A feedback', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'aMixer_outB', name = 'A -> B', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'aMixer_outC', name = 'A -> C', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'aMixer_inB', name = 'A <- B', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'aMixer_inC', name = 'A <- C', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'aProcess_delayTime', name = 'A delay time', type = 'control', min = 0.005, max = 3, warp = 'lin', step = 0.01, quantum = 0.01, default = 0.1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," sec")) end},
      {id = 'aProcess_shiftFreq', name = 'A frequency shift', type = 'control', min = 0, max = 1200, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'aProcess_lSHz', name = 'A low shelf', type = 'control', min = 20, max = 12000, warp = 'exp', default = 600, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'aProcess_lSdb', name = 'A low shelf gain', type = 'control', min = -15, max = 15, warp = 'lin', default = 0, quantum = 1/30, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," dB")) end},
      {id = 'aProcess_lSQ', name = 'A low shelf q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'aProcess_hSHz', name = 'A hi shelf', type = 'control', min = 80, max = 19000, warp = 'exp', default = 19000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'aProcess_hSdb', name = 'A hi shelf gain', type = 'control', min = -15, max = 15, warp = 'lin', default = 0, quantum = 1/30, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," dB")) end},
      {id = 'aProcess_hSQ', name = 'A hi shelf q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {type = 'separator', name = 'B'},
      {id = 'bMixer_inAmp', name = 'B <- engine', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'mainMixer_inB', name = 'B -> mixer', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'bMixer_inB', name = 'B feedback', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'bMixer_outA', name = 'B -> A', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'bMixer_outC', name = 'B -> C', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'bMixer_inA', name = 'B <- A', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'bMixer_inC', name = 'B <- C', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'bProcess_delayTime', name = 'B delay time', type = 'control', min = 0.005, max = 3, warp = 'lin', step = 0.01, quantum = 0.01, default = 0.1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," sec")) end},
      {id = 'bProcess_shiftFreq', name = 'B frequency shift', type = 'control', min = 0, max = 1200, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'bProcess_lSHz', name = 'B low shelf', type = 'control', min = 20, max = 12000, warp = 'exp', default = 600, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'bProcess_lSdb', name = 'B low shelf gain', type = 'control', min = -15, max = 15, warp = 'lin', default = 0, quantum = 1/30, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," dB")) end},
      {id = 'bProcess_lSQ', name = 'B low shelf q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'bProcess_hSHz', name = 'B hi shelf', type = 'control', min = 80, max = 19000, warp = 'exp', default = 19000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'bProcess_hSdb', name = 'B hi shelf gain', type = 'control', min = -15, max = 15, warp = 'lin', default = 0, quantum = 1/30, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," dB")) end},
      {id = 'bProcess_hSQ', name = 'B hi shelf q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {type = 'separator', name = 'C'},
      {id = 'cMixer_inAmp', name = 'C <- engine', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'mainMixer_inC', name = 'C -> mixer', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'cMixer_inC', name = 'C feedback', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'cMixer_outA', name = 'C -> A', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'cMixer_outB', name = 'C -> B', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'cMixer_inA', name = 'C <- A', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'cMixer_inB', name = 'C <- B', type = 'control', min = 0, max = 2, warp = 'lin', step = 0.01, quantum = 0.01, default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'cProcess_delayTime', name = 'C delay time', type = 'control', min = 0.005, max = 3, warp = 'lin', step = 0.01, quantum = 0.01, default = 0.1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," sec")) end},
      {id = 'cProcess_shiftFreq', name = 'C frequency shift', type = 'control', min = 0, max = 1200, warp = 'lin', default = 0, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'cProcess_lSHz', name = 'C low shelf', type = 'control', min = 20, max = 12000, warp = 'exp', default = 600, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'cProcess_lSdb', name = 'C low shelf gain', type = 'control', min = -15, max = 15, warp = 'lin', default = 0, quantum = 1/30, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," dB")) end},
      {id = 'cProcess_lSQ', name = 'C low shelf q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'cProcess_hSHz', name = 'C hi shelf', type = 'control', min = 80, max = 19000, warp = 'exp', default = 19000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'cProcess_hSdb', name = 'C hi shelf gain', type = 'control', min = -15, max = 15, warp = 'lin', default = 0, quantum = 1/30, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," dB")) end},
      {id = 'cProcess_hSQ', name = 'C hi shelf q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
    },
    ["main"] = {
      {type = 'separator', name = 'main output settings'},
      {id = 'lSHz', name = 'low shelf', type = 'control', min = 20, max = 12000, warp = 'exp', default = 600, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'lSdb', name = 'low shelf gain', type = 'control', min = -15, max = 15, warp = 'lin', default = 0, quantum = 1/30, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," dB")) end},
      {id = 'lSQ', name = 'low shelf q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'hSHz', name = 'hi shelf', type = 'control', min = 800, max = 19000, warp = 'exp', default = 19000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'hSdb', name = 'hi shelf gain', type = 'control', min = -15, max = 15, warp = 'lin', default = 0, quantum = 1/30, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," dB")) end},
      {id = 'hSQ', name = 'hi shelf q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'eqHz', name = 'eq', type = 'control', min = 20, max = 24000, warp = 'exp', default = 6000, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," Hz")) end},
      {id = 'eqdb', name = 'eq gain', type = 'control', min = -30, max = 15, warp = 'lin', default = 0, quantum = 1/45, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),0.01," dB")) end},
      {id = 'eqQ', name = 'eq q', type = 'control', min = 0, max = 100, warp = 'lin', default = 50, quantum = 1/100, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param),1,"%")) end},
      {id = 'limiterLevel', name = 'limiter level', type = 'control', min = 0, max = 2, warp = 'lin', default = 0.5, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
      {id = 'level', name = 'final output level', type = 'control', min = 0, max = 2, warp = 'lin', default = 1, formatter = function(param) return (round_form((type(param) == 'table' and param:get() or param)*100,1,"%")) end},
    }
  }

  params:add_separator("kildare_voice_header","kildare voices")

  if engine.name ~= "Kildare" then
    params:add_option("no_kildare","----- kildare not loaded -----",{" "})
  end

  params:add_group('kildare_model_management', 'models', 7)
  local models = {'bd','sd','tm','cp','rs','cb','hh','saw','fld'}
  for i = 1,7 do
    params:add_option('voice_model_'..i, 'voice '..i, models, i)
    params:set_action('voice_model_'..i, function(x)
      Kildare.rebuild_model_params(i, models[x])
      engine.set_model(i, 'kildare_'..models[x])
    end)
  end

  local custom_actions = {'carHz', 'poly', 'sampleMode', 'sampleFile', 'sampleClear', 'playbackRateBase', 'playbackRateOffset', 'playbackPitchControl', 'loop'}
  
  local how_many_params = 0
  for i = 1,#swappable_drums do
    how_many_params = tab.count(kildare_drum_params[swappable_drums[i]]) + how_many_params
  end

  for i = 1,7 do
    local shown_set = params:string('voice_model_'..i)
    params:add_group('kildare_'..i..'_group', i..': '..shown_set, how_many_params)
    
    for k,v in pairs(swappable_drums) do
      for prms,d in pairs(kildare_drum_params[v]) do
        if d.type == 'control' then
          local quantum_size = 0.01
          if d.quantum ~= nil then
            quantum_size = d.quantum
          end
          local step_size = 0
          if d.step ~= nil then
            step_size = d.step
          end
          if d.id == "carHz" then
            quantum_size = 1/math.abs(d.max-d.min)
          end
          params:add_control(
            i..'_'..v..'_'..d.id,
            d.name,
            ControlSpec.new(d.min, d.max, d.warp, step_size, d.default, nil, quantum_size),
            d.formatter
          )
        elseif d.type == 'number' then
          params:add_number(
            i..'_'..v..'_'..d.id,
            d.name,
            d.min,
            d.max,
            d.default,
            d.formatter
          )
        elseif d.type == "option" then
          params:add_option(
            i..'_'..v..'_'..d.id,
            d.name,
            d.options,
            d.default
          )
        elseif d.type == 'separator' then
          params:add_separator(i..'_separator_'..v..'_'..d.name, d.name)
        elseif d.type == 'file' then
          params:add_file(i.."_"..v..'_'..d.id, d.name, d.default)
        elseif d.type == 'binary' then
          params:add_binary(i.."_"..v..'_'..d.id, d.name, d.behavior)
        end
        if d.type ~= 'separator' then
          if not tab.contains(custom_actions,d.id) then
            params:set_action(i.."_"..v..'_'..d.id, function(x)
              if engine.name == "Kildare" then
                if v == params:string('voice_model_'..i) then
                  engine.set_voice_param(i, d.id, x)
                  Kildare.voice_param_callback(i, d.id, x)
                end
              end
            end)
          elseif d.id == "carHz" then
            params:set_action(i.."_"..v..'_'..d.id, function(x)
              if engine.name == "Kildare" then
                if v == params:string('voice_model_'..i) then
                  engine.set_voice_param(i, d.id, musicutil.note_num_to_freq(x))
                  engine.set_voice_param(i, 'thirdHz', musicutil.note_num_to_freq(x))
                  engine.set_voice_param(i, 'seventhHz', musicutil.note_num_to_freq(x))
                  Kildare.voice_param_callback(i, d.id, x)
                end
              end
            end)
          elseif d.id == "poly" then
            params:set_action(i.."_"..v..'_'..d.id, function(x)
              if engine.name == "Kildare" then
                if v == params:string('voice_model_'..i) then
                  engine.set_voice_param(i, d.id, x == 1 and 0 or 1)
                  Kildare.voice_param_callback(i, d.id, x)
                end
              end
            end)
            if not poly then
              params:hide(i.."_"..v..'_'..d.id) -- avoid exposing poly for performance management
            end
          end
        end
      end
    end

    Kildare.rebuild_model_params(i,shown_set)

  end

  for j = 8,10 do
    local k = 'sample'..(j-7)
    j = 'sample'..(j-7)
    params:add_group('kildare_'..j..'_group', j, #kildare_drum_params[k])
    
    for i = 1, #kildare_drum_params[k] do
      local d = kildare_drum_params[k][i]
      if d.type == 'control' then
        local quantum_size = 0.01
        if d.quantum ~= nil then
          quantum_size = d.quantum
        end
        local step_size = 0
        if d.step ~= nil then
          step_size = d.step
        end
        if d.id == "carHz" then
          quantum_size = 1/math.abs(d.max-d.min)
        end
        params:add_control(
          j.."_"..d.id,
          d.name,
          ControlSpec.new(d.min, d.max, d.warp, step_size, d.default, nil, quantum_size),
          d.formatter
        )
      elseif d.type == 'number' then
        params:add_number(
          j.."_"..d.id,
          d.name,
          d.min,
          d.max,
          d.default,
          d.formatter
        )
      elseif d.type == "option" then
        params:add_option(
          j.."_"..d.id,
          d.name,
          d.options,
          d.default
        )
      elseif d.type == 'separator' then
        params:add_separator(j..'_separator_'..d.name, d.name)
      elseif d.type == 'file' then
        params:add_file(j.."_"..d.id, d.name, d.default)
      elseif d.type == 'binary' then
        params:add_binary(j.."_"..d.id, d.name, d.behavior)
      end
      if d.type ~= 'separator' then
        if not tab.contains(custom_actions,d.id) then
          params:set_action(j.."_"..d.id, function(x)
            if engine.name == "Kildare" then
              engine.set_voice_param(j, d.id, x)
              Kildare.voice_param_callback(j, d.id, x)
            end
          end)
        elseif d.id == "carHz" then
          params:set_action(j.."_"..d.id, function(x)
            if engine.name == "Kildare" then
              engine.set_voice_param(j, d.id, musicutil.note_num_to_freq(x))
              Kildare.voice_param_callback(j, d.id, x)
            end
          end)
        elseif d.id == "poly" then
          params:set_action(j.."_"..d.id, function(x)
            if engine.name == "Kildare" then
              engine.set_voice_param(j, d.id, x == 1 and 0 or 1)
              Kildare.voice_param_callback(j, d.id, x)
            end
          end)
          if not poly then
            params:hide(j.."_"..d.id) -- avoid exposing poly for performance management
          end
        elseif d.id == "sampleMode" then
        elseif d.id == "sampleFile" then
          params:set_action(j.."_"..d.id,
            function(file)
              if file ~= _path.audio then
                if params:string(j.."_sampleMode") == "distribute" then
                  local split_at = string.match(file, "^.*()/")
                  local folder = string.sub(file, 1, split_at)
                  engine.load_folder(j,folder)
                  Kildare.folder_callback(j,folder)
                else
                  engine.load_file(j,file)
                  Kildare.file_callback(j,file)
                end
              end
            end
          )
        elseif d.id == "sampleClear" then
          params:set_action(j.."_"..d.id,
            function(x)
              engine.clear_samples(j)
              params:set(j.."_sampleFile", _path.audio, silent)
              Kildare.clear_callback(j)
            end
          )
        elseif d.id == 'playbackRateBase' then
        elseif d.id == 'playbackRateOffset' or d.id == 'playbackPitchControl' then
        elseif d.id == 'loop' then
          params:set_action(j..'_loop',
            function(x)
              if x == 0 then
                engine.stop_sample(j)
              end
            end
          )
        end
      end
    end
  end

  local function build_slices(path,slices,sample_voice)
    local ch, len, smp = audio.file_info(path)
    local dur = len/smp
    local per_slice_dur = dur/slices
    local split_at = string.match(path, "^.*()/")
    local folder = string.sub(path, 1, split_at)
    local filename = path:match("^.+/(.+)$")
    local filename_raw = filename:match("(.+)%..+")

    if params:string('kildare_st_chop_length') == 'current bpm' then
      local synced_length = util.round_up((dur) - (dur * ((slices-1)/slices)), clock.get_beat_sec())
      if clock.get_beat_sec()*slices > dur then
        if (clock.get_beat_sec()/2)*slices > dur then
          if (clock.get_beat_sec()/3)*slices > dur then
            if (clock.get_beat_sec()/4)*slices > dur then
              synced_length = synced_length / 4
              -- print('sixteenth notes')
            end
          else
            synced_length = synced_length / 3
            -- print('twelfth notes')
          end
        else
          synced_length = synced_length / 2
          -- print('eighth notes')
        end
      else
        -- print('quarter notes')
      end
      per_slice_dur = synced_length
    end

    if per_slice_dur > 0.02 then
      print(folder, filename_raw)
      local parent_folder = _path.audio..'kildare/TEMP/'..filename_raw..'-'..os.date("%Y%m%d_%H-%M-%S")..'/'
      if util.file_exists(parent_folder) then
        norns.system_cmd('rm -r '..parent_folder)
      end
      norns.system_cmd('mkdir -p '..parent_folder)
      local new_name = parent_folder..filename_raw..'%2n.flac'
      norns.system_cmd('sox '..path..' '..new_name..' trim 0 '..per_slice_dur..' fade 0:00.01 -0 0:00.01 : newfile : restart')
      clock.run(function()
        clock.sleep(0.3)
        params:set(sample_voice..'_sampleClear',1)
        params:set(sample_voice..'_sampleClear',0)
        params:set(sample_voice..'_sampleMode',3)
        params:set(sample_voice..'_sampleFile',parent_folder..filename_raw..'01.flac')
      end)
    else
      print('kildare: sample duration too small to fade')
    end
  end

  if sox_installed then
    params:add_group('kildare_st_header','kildare sample tools',6)
    params:add_separator('kildare_st_notice', "for 'distribute' sample mode")
    params:add_text('kildare_st_info', '')
    params:hide('kildare_st_info')
    _menu.rebuild_params()
    params:add_file('kildare_st_chop','chop w/ fade', _path.audio)
    params:set_action('kildare_st_chop',
    function(file)
      if file ~= _path.audio and file ~= '' then
        build_slices(file, params:get('kildare_st_chop_count'), params:string('kildare_st_preload'))
        params:set('kildare_st_chop', '', true)
        params:set('kildare_st_info', '~~~ chopping '..file:match("^.+/(.+)$"))
        params:show('kildare_st_info')
        _menu.rebuild_params()
        clock.run(function()
          clock.sleep(2)
          params:set('kildare_st_info', '  ~~~ chopped! ~~~')
          clock.sleep(2)
          params:hide('kildare_st_info')
          _menu.rebuild_params()
        end)
      end
    end)
    params:add_number('kildare_st_chop_count', '   chop count', 2, 48, 16)
    params:add_option('kildare_st_chop_length', '   length factor', {'even chops', 'current bpm'})
    params:add_option('kildare_st_preload', '   preload to ', {'sample1','sample2','sample3'},1)
  end

  params:add_separator("kildare_fx_header","kildare fx")

  for j = 1,#Kildare.fx do
    local k = Kildare.fx[j]
    params:add_group('kildare_'..k, k, #kildare_fx_params[k])
    for i = 1, #kildare_fx_params[k] do
      local d = kildare_fx_params[k][i]
      if d.type == 'control' then
        local quantum_size = 0.01
        if d.quantum ~= nil then
          quantum_size = d.quantum
        end
        local step_size = 0
        if d.step ~= nil then
          step_size = d.step
        end
        params:add_control(
          k.."_"..d.id,
          d.name,
          ControlSpec.new(d.min, d.max, d.warp, step_size, d.default, nil, quantum_size),
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
        params:add_separator('kildare_fx_params_'..d.name, d.name)
      end
      if d.type ~= 'separator' then
        params:set_action(k.."_"..d.id, function(x)
          if engine.name == "Kildare" then
            if k == "delay" and d.id == "time" then
              engine["set_"..k.."_param"](d.id, clock.get_beat_sec() * x/128)
            elseif k ~= 'feedback' then
              engine["set_"..k.."_param"](d.id, x)
            elseif k == 'feedback' then
              local sub = '_'
              local keys = {}
              for str in string.gmatch(d.id, "([^"..sub.."]+)") do
                table.insert(keys,str)
              end
              local targetKey = keys[1]
              local paramKey = keys[2]
              -- print(targetKey, paramKey)
              local targetLine = string.upper(string.sub(targetKey, 1, 1))
              if paramKey == 'outA' then
                params:set('feedback_aMixer_in'..targetLine, x)
              elseif paramKey == 'outB' then
                params:set('feedback_bMixer_in'..targetLine, x)
              elseif paramKey == 'outC' then
                params:set('feedback_cMixer_in'..targetLine, x)
              end
              engine['set_feedback_param'](targetKey, paramKey, x)
            end
          end
        end)
      end
    end
  end

  params:hide('feedback_aMixer_inB')
  params:hide('feedback_aMixer_inC')
  params:hide('feedback_bMixer_inA')
  params:hide('feedback_bMixer_inC')
  params:hide('feedback_cMixer_inA')
  params:hide('feedback_cMixer_inB')

  _menu.rebuild_params()

  params:add_separator("kildare_lfo_header","kildare lfos")
  Kildare.lfos.add_params(Kildare.drums, Kildare.fx ,poly)
  
  params:bang()

  Kildare.loaded = true
  
end

return Kildare