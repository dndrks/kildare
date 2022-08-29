-- adapted from @markeats

local musicutil = require 'musicutil'

lfos = {}
lfos.count = 32

lfos.rates = {1/16,1/8,1/4,5/16,1/3,3/8,1/2,3/4,1,1.5,2,3,4,6,8,16,32,64,128,256,512,1024}
lfos.rates_as_strings = {"1/16","1/8","1/4","5/16","1/3","3/8","1/2","3/4","1","1.5","2","3","4","6","8","16","32","64","128","256","512","1024"}

lfos.targets = {}
lfos.current_baseline = {}
lfos.specs = {}

lfos.params_list = {}
lfos.last_param = {}
for i = 1,lfos.count do
  lfos.last_param[i] = "empty"
end

_lfo = require 'lfo'

klfo = {}

ivals = {}

function lfos.add_params(drum_names, fx_names, poly)

  for i = 1,lfos.count do
    klfo[i] = _lfo:add{
      action = 
      function(s,r)
        local target_track = params:string("lfo_target_track_"..i)
        local target_param = params:get("lfo_target_param_"..i)
        local param_name = lfos.params_list[target_track]
        if not lfos.current_baseline[i] then
          lfos.send_param_value(target_track, param_name.ids[(target_param)], s)
        else
          -- local current_centroid = math.abs(params:get("lfo_min_"..i) - params:get("lfo_max_"..i)) * klfo[i].depth/2
          local current_centroid = math.abs(params:get("lfo_min_"..i) - params:get("lfo_max_"..i)) * klfo[i].depth
          local scaled_min = util.clamp(params:get(target_track..'_'..lfos.params_list[target_track].ids[target_param]) - current_centroid, params:get("lfo_min_"..i), params:get("lfo_max_"..i))
          local scaled_max = util.clamp(params:get(target_track..'_'..lfos.params_list[target_track].ids[target_param]) + current_centroid, params:get("lfo_min_"..i), params:get("lfo_max_"..i))
          r = util.linlin(0,1,scaled_min, scaled_max,r + klfo[i].offset)
          lfos.send_param_value(target_track, param_name.ids[(target_param)], r)
        end
      end,
      ppqn = 32
    }
  end

  for k,v in pairs(drum_names) do
    table.insert(lfos.targets,k <= 7 and k or v)
  end

  for k,v in pairs(fx_names) do
    table.insert(lfos.targets,v)
  end

  for k,v in pairs(lfos.targets) do
    ivals[v] = {1 + (16*(k-1)), (16 * k)}
  end

  for fx,parameters in pairs(kildare_fx_params) do
    lfos[fx.."_params"] = {}
    for inner,prm in pairs(kildare_fx_params[fx]) do
      if prm.type ~= "separator" then
        table.insert(lfos[fx.."_params"], prm.id)
      end
    end
  end

  for k,v in pairs(ivals) do
    lfos.rebuild_model_spec(k,poly)
  end

  lfos.build_params_static(poly)

  params:add_group("lfos",lfos.count * 15)
  for i = 1,lfos.count do
    if lfos.targets[util.wrap(i,1,#lfos.targets)] == "delay" then
      lfos.last_param[i] = "time"
    elseif lfos.targets[util.wrap(i,1,#lfos.targets)] == "reverb" then
      lfos.last_param[i] = "decay"
    elseif lfos.targets[util.wrap(i,1,#lfos.targets)] == "main" then
      lfos.last_param[i] = "lSHz"
    elseif lfos.targets[util.wrap(i,1,#lfos.targets)] == "sample1" or
    lfos.targets[util.wrap(i,1,#lfos.targets)] == "sample2" or
    lfos.targets[util.wrap(i,1,#lfos.targets)] == "sample3" then
      lfos.last_param[i] = "poly"
    else
      if poly then
        lfos.last_param[i] = "poly"
      else
        lfos.last_param[i] = "amp"
      end
    end
    
    params:add_separator('lfo_'..i..'_separator', "lfo "..i)
    
    params:add_option("lfo_"..i,"state",{"off","on"},1)
    params:set_action("lfo_"..i,function(x)
      if x == 1 then
        klfo[i]:stop()
        lfos.return_to_baseline(i,true,true)
        params:hide("lfo_target_track_"..i)
        params:hide("lfo_target_param_"..i)
        params:hide("lfo_shape_"..i)
        params:hide("lfo_beats_"..i)
        params:hide("lfo_free_"..i)
        params:hide("lfo_offset_"..i)
        params:hide("lfo_depth_"..i)
        params:hide("lfo_min_"..i)
        params:hide("lfo_max_"..i)
        params:hide("lfo_mode_"..i)
        params:hide("lfo_baseline_"..i)
        params:hide("lfo_reset_"..i)
        params:hide("lfo_reset_target_"..i)
        _menu.rebuild_params()
      elseif x == 2 then
        klfo[i]:start()
        params:show("lfo_target_track_"..i)
        params:show("lfo_target_param_"..i)
        params:show("lfo_shape_"..i)
        if params:get("lfo_mode_"..i) == 1 then
          params:show("lfo_beats_"..i)
        else
          params:show("lfo_free_"..i)
        end
        params:show("lfo_offset_"..i)
        params:show("lfo_depth_"..i)
        params:show("lfo_min_"..i)
        params:show("lfo_max_"..i)
        params:show("lfo_mode_"..i)
        params:show("lfo_baseline_"..i)
        params:show("lfo_reset_"..i)
        params:show("lfo_reset_target_"..i)
        _menu.rebuild_params()
      end
    end)

    params:add_option("lfo_target_track_"..i, "track", lfos.targets, 1)
    params:set_action("lfo_target_track_"..i,
      function(x)
        lfos.change_target(i)
      end
    )

    params:add_option("lfo_target_param_"..i, "param",lfos.params_list[lfos.targets[1]].names,1)
    params:set_action("lfo_target_param_"..i,
      function(x)
        lfos.rebuild_param("min",i)
        lfos.rebuild_param("max",i)
        lfos.return_to_baseline(i,nil,true)
        lfos.reset_bounds_in_menu(i)
      end
    )

    params:add_option("lfo_shape_"..i, "shape", {"sine","saw","square","random"},1)
    params:set_action('lfo_shape_'..i, function(x)
      klfo[i]:set('shape', params:lookup_param("lfo_shape_"..i).options[x])
    end)

    params:add_option("lfo_beats_"..i, "rate", lfos.rates_as_strings, tab.key(lfos.rates_as_strings,"1"))
    params:set_action("lfo_beats_"..i,
      function(x)
        if params:string("lfo_mode_"..i) == "clocked bars" then
          klfo[i]:set('period',lfos.rates[x] * 4)
        end
      end
    )

    params:add{
      type = 'control',
      id = "lfo_free_"..i,
      name = "rate",
      controlspec = controlspec.new(0.1,300,'exp',0.1,1,'sec')
    }
    params:set_action("lfo_free_"..i,
      function(x)
        if params:string("lfo_mode_"..i) == "free" then
          klfo[i]:set('period',x)
        end
      end
    )

    params:add_number("lfo_depth_"..i,"depth",0,100,0,function(param) return (param:get().."%") end)
    params:set_action("lfo_depth_"..i, function(x)
      klfo[i]:set('depth',x/100)
      if x == 0 then
        lfos.return_to_baseline(i,true,true)
      end
    end)

    params:add_number('lfo_offset_'..i, 'lfo offset', -100, 100, 0, function(param) return (param:get().."%") end)
    params:set_action("lfo_offset_"..i, function(x)
      klfo[i]:set('offset',x/100)
    end)

    local target_track = params:string("lfo_target_track_"..i)
    local target_param = params:get("lfo_target_param_"..i)
    params:add{
      type='control',
      id="lfo_min_"..i,
      name="lfo min",
      controlspec = controlspec.new(
        lfos.specs[target_track][target_param].min,
        lfos.specs[target_track][target_param].max,
        lfos.specs[target_track][target_param].warp,
        lfos.specs[target_track][target_param].step,
        lfos.specs[target_track][target_param].min,
        '',
        lfos.specs[target_track][target_param].quantum
      )
    }
    params:set_action('lfo_min_'..i, function(x)
      klfo[i]:set('min',x)
    end)

    params:add{
      type='control',
      id="lfo_max_"..i,
      name="lfo max",
      controlspec = controlspec.new(
        lfos.specs[target_track][target_param].min,
        lfos.specs[target_track][target_param].max,
        lfos.specs[target_track][target_param].warp,
        lfos.specs[target_track][target_param].step,
        lfos.specs[target_track][target_param].default,
        '',
        lfos.specs[target_track][target_param].quantum
      )
    }
    params:set_action('lfo_max_'..i, function(x)
      klfo[i]:set('max',x)
    end)

    params:add_option("lfo_mode_"..i, "update mode", {"clocked bars","free"},1)
    params:set_action("lfo_mode_"..i,
      function(x)
        if x == 1 and params:string("lfo_"..i) == "on" then
          params:hide("lfo_free_"..i)
          params:show("lfo_beats_"..i)
          klfo[i]:set('mode', 'clocked')
        elseif x == 2 then
          params:hide("lfo_beats_"..i)
          params:show("lfo_free_"..i)
          klfo[i]:set('mode', 'free')
        end
        _menu.rebuild_params()
      end
    )

    local baseline_options;
    baseline_options = {"from min", "from center", "from max", 'from current'}
    params:add_option("lfo_baseline_"..i, "lfo baseline", baseline_options, 1)
    params:set_action("lfo_baseline_"..i, function(x)
      if x ~= 4 then
        lfos.current_baseline[i] = false
        klfo[i]:set('baseline',string.gsub(params:lookup_param("lfo_baseline_"..i).options[x],"from ",""))
      else
        lfos.current_baseline[i] = true
        klfo[i]:set('baseline','min')
      end
    end)

    params:add_trigger("lfo_reset_"..i, "reset lfo")
    params:set_action("lfo_reset_"..i, function() klfo[i]:reset_phase() end)

    params:add_option("lfo_reset_target_"..i, "reset lfo to", {"floor","ceiling"}, 1)
    params:set_action("lfo_reset_target_"..i, function(x)
      klfo[i]:set('reset_target', params:lookup_param("lfo_reset_target_"..i).options[x])
    end)

    -- params:hide("lfo_free_"..i)
    -- params:hide("lfo_beats_"..i)
    _menu.rebuild_params()

  end

end

function lfos.reset_bounds_in_menu(i)
  local target_track = params:string("lfo_target_track_"..i)
  local target_param = params:get("lfo_target_param_"..i)
  local restore_min = lfos.specs[target_track][target_param].min
  local restore_max;
  if params:get("lfo_target_track_"..i) <= 7 then
    restore_max = params:get(target_track.."_"..params:string('voice_model_'..params:get("lfo_target_track_"..i))..'_'..lfos.params_list[target_track].ids[(target_param)])
  else
    restore_max = params:get(target_track.."_"..lfos.params_list[target_track].ids[(target_param)])
  end
  if restore_min == restore_max then
    restore_max = lfos.specs[target_track][target_param].max
  end
  if params:string("lfo_target_param_"..i) == "pan" then
    restore_max = 1
  end
  params:set("lfo_min_"..i, restore_min)
  params:set("lfo_max_"..i, restore_max)
end

function lfos.change_target(i)
  local param_id = params.lookup["lfo_target_param_"..i]
  params.params[param_id].options = lfos.params_list[lfos.targets[params:get("lfo_target_track_"..i)]].names
  params.params[param_id].count = tab.count(params.params[param_id].options)
  lfos.rebuild_param("min",i)
  lfos.rebuild_param("max",i)
  lfos.return_to_baseline(i,nil,true)
  params:set("lfo_target_param_"..i,1)
  params:set("lfo_depth_"..i,0)
  lfos.reset_bounds_in_menu(i)
end

function lfos.return_to_baseline(i,silent,poly)
  local drum_target = params:get("lfo_target_track_"..i)
  local parent = lfos.targets[drum_target]
  local param_name = parent.."_"..(lfos.params_list[parent].ids[(params:get("lfo_target_param_"..i))])
  local param_exclusions = {'delay','reverb','main','sample1','sample2','sample3'}
  if not tab.contains(param_exclusions, parent) then
    if lfos.last_param[i] == "time" or lfos.last_param[i] == "decay" or lfos.last_param[i] == "lSHz" or lfos.last_param[i] == "sampleMode" then
      if poly then
        lfos.last_param[i] = "poly"
      else
        lfos.last_param[i] = "amp"
      end
    end
    local focus_voice = params:string('voice_model_'..parent)
    if lfos.last_param[i] ~= "carHz" and lfos.last_param[i] ~= "poly" and engine.name == "Kildare" then
      engine.set_voice_param(parent,lfos.last_param[i],params:get(parent.."_"..focus_voice..'_'..lfos.last_param[i]))
    elseif lfos.last_param[i] == "carHz" and engine.name == "Kildare" then
      engine.set_voice_param(parent,lfos.last_param[i],musicutil.note_num_to_freq(params:get(parent.."_"..focus_voice..'_'..lfos.last_param[i])))
    elseif lfos.last_param[i] == "poly" and engine.name == "Kildare" then
      engine.set_voice_param(parent,lfos.last_param[i],params:get(parent.."_"..focus_voice..'_'..lfos.last_param[i]) == 1 and 0 or 1)
    end
  elseif (parent == "delay" or parent == "reverb" or parent == "main") and engine.name == "Kildare" then
    local sources = {delay = lfos.delay_params, reverb = lfos.reverb_params, main = lfos.main_params}
    if not tab.contains(sources[parent],lfos.last_param[i]) then
      lfos.last_param[i] = sources[parent][1]
    end
    if parent == "delay" and lfos.last_param[i] == "time" then
      engine["set_"..parent.."_param"](lfos.last_param[i],clock.get_beat_sec() * params:get(parent.."_"..lfos.last_param[i])/128)
    else
      engine["set_"..parent.."_param"](lfos.last_param[i],params:get(parent.."_"..lfos.last_param[i]))
    end
  elseif (parent == "sample1" or parent == "sample2" or parent == "sample3") and engine.name == "Kildare" then
    if lfos.last_param[i] == "time" or lfos.last_param[i] == "decay" or lfos.last_param[i] == "lSHz" or lfos.last_param[i] == "sampleMode" then
      if poly then
        lfos.last_param[i] = "poly"
      else
        lfos.last_param[i] = "amp"
      end
    end
    if lfos.last_param[i] == "poly" then
      engine["set_voice_param"](parent,lfos.last_param[i],params:get(parent.."_"..lfos.last_param[i]) == 1 and 0 or 1)
    else
      engine["set_voice_param"](parent,lfos.last_param[i],params:get(parent.."_"..lfos.last_param[i]))
    end
  end
  if not silent then
    lfos.last_param[i] = (lfos.params_list[parent].ids[(params:get("lfo_target_param_"..i))])
  end
end

function lfos.rebuild_param(param,i)
  local param_id = params.lookup["lfo_"..param.."_"..i]
  local target_track = params:string("lfo_target_track_"..i)
  local target_param = params:get("lfo_target_param_"..i)
  local default_value;
  if params:get("lfo_target_track_"..i) <= 7 then
    default_value = param == "min" and lfos.specs[target_track][target_param].min
    or params:get(target_track.."_"..params:string('voice_model_'..params:string("lfo_target_track_"..i))..'_'..lfos.params_list[target_track].ids[(target_param)])
  else
    default_value = param == "min" and lfos.specs[target_track][target_param].min
    or params:get(target_track.."_"..lfos.params_list[target_track].ids[(target_param)])
  end
  if param == "max" then
    if lfos.specs[target_track][target_param].min == default_value then
      default_value = lfos.specs[target_track][target_param].max
    end
  end
  params.params[param_id].controlspec = controlspec.new(
    lfos.specs[target_track][target_param].min,
    lfos.specs[target_track][target_param].max,
    lfos.specs[target_track][target_param].warp,
    lfos.specs[target_track][target_param].step,
    default_value,
    '',
    lfos.specs[target_track][target_param].quantum
  )
  if param == "max" then
    if params:string("lfo_target_param_"..i) == "pan" then
      default_value = 1
    end
    params.params[param_id]:set_raw(params.params[param_id].controlspec:unmap(default_value))
  end
  if lfos.specs[target_track][target_param].formatter ~= nil then
    params.params[param_id].formatter = lfos.specs[target_track][target_param].formatter
  end
  if param == 'min' then
    klfo[i]:set(param,lfos.specs[target_track][target_param][param])
  else
    klfo[i]:set(param,default_value)
  end
end

function lfos.build_params_static(poly)
  for i = 1,#lfos.targets do
    local style = lfos.targets[i]
    lfos.params_list[style] = {ids = {}, names = {}}
    local focus_voice;
    if type(style) == 'number' then
      focus_voice = params:string('voice_model_'..style)
    else
      focus_voice = style
    end
    local parent = (style ~= "delay" and style ~= "reverb" and style ~= "main") and kildare_drum_params[focus_voice] or kildare_fx_params[focus_voice]
    for j = 1,#parent do
      if parent[j].type ~= "separator" and parent[j].lfo_exclude == nil then
        if (parent[j].id == "poly" and poly) or (parent[j].id ~= "poly") then
          table.insert(lfos.params_list[style].ids, parent[j].id)
          table.insert(lfos.params_list[style].names, parent[j].name)
        end
      end
    end

  end
end

function lfos.set_delay_param(param_target,value)
  if param_target == "time" then
    engine.set_delay_param(param_target,clock.get_beat_sec() * value/128)
  else
    engine.set_delay_param(param_target,value)
  end
end

function lfos.send_param_value(target_track, target_id, value)
  if target_track ~= "delay" and target_track ~= "reverb" and target_track ~= "main" then
    if target_id == "carHz" then
      value = musicutil.note_num_to_freq(value)
    end
    if string.find(target_track,'sample') and (target_id == 'playbackRateBase' or target_id == 'loop') then
      params:set(target_track..'_'..target_id,util.round(value))
    else
      engine.set_voice_param(target_track,target_id,value)
    end
  else
    if target_track == "delay" then
      lfos.set_delay_param(target_id,value)
    else
      engine["set_"..target_track.."_param"](target_id,value)
    end
  end
end

function lfos.rebuild_model_spec(k,poly)
  lfos.specs[k] = {}
  local i = 1

  -- t values:
  -- 0: separator
  -- 1: number
  -- 2: option
  -- 3: control
  -- 4: file
  -- 5: taper
  -- 6: trigger
  -- 7: group
  -- 8: text
  -- 9: binary

  local focus_voice;
  if type(k) == 'number' then
    focus_voice = params:string('voice_model_'..k)
  else
    focus_voice = k
  end
  local param_group = (k ~= "delay" and k ~= "reverb" and k ~= "main") and kildare_drum_params or kildare_fx_params
  for key,val in pairs(param_group[focus_voice]) do
    if param_group[focus_voice][key].type ~= "separator" then
      if param_group[focus_voice][key].lfo_exclude == nil then
        if (poly == nil and val.id ~= "poly") or (poly == true) then
          local concat_name = type(k) == 'number' and (k.."_"..focus_voice..'_'..param_group[focus_voice][key].id) or (k.."_"..param_group[focus_voice][key].id)
          local system_id = params.lookup[concat_name]
          local quantum_size;
          if params.params[system_id].controlspec ~= nil then
            quantum_size = params.params[system_id].controlspec.quantum
          else
            quantum_size = param_group[focus_voice][key].quantum ~= nil and param_group[focus_voice][key].quantum or 0.01
          end
          lfos.specs[k][i] = {
            min = param_group[focus_voice][key].min,
            max = param_group[focus_voice][key].max,
            warp = param_group[focus_voice][key].warp,
            step = 0,
            default = param_group[focus_voice][key].default,
            quantum = quantum_size,
            formatter = param_group[focus_voice][key].formatter
          }
          i = i+1 -- do not increment by the separators' gaps...
        end
      end
    end
  end
end

return lfos