-- nonrealtime changes test

engine.name = "NRTchanges"

function init()
  clock.run(
      function()
        while true do
          clock.sync(1)
          engine.cutoff(math.random(200,5000))
        end
      end
  )
end

function key(n,z)
  if z == 1 and n == 3 then
    engine.hz(math.random(200,300))
  end
end
