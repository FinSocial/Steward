// Returns new jQuery obj for the name of a pool
function makeNewPool(name) {
   let ret = $(`<div class="list-group-item list-group-item-action pool">         
              <span class="portName">${name}</span>
              <!--<a class="actionButton editPort float-right fa fa-pencil" aria-hidden="true"></a>
              <a class="actionButton deletePort float-right fa fa-trash" aria-hidden="true"></a>-->
          </div>`);
   return ret;
}

function poolClickHandler(e) {
  let elm = $(e.target);
 	$('.pool').removeClass('active');
 	elm.addClass('active');
  const portName = elm.children('.portName')[0].innerText;
  getStocks(portName);
  graph.update(portName);
}

$('.pool').click(poolClickHandler);

// Add portfolio button
$('#joinPool').click((e) => {
    if($('#newPool').length == 0) {
        $('#pools').append('<div class="form-group list-group-item list-group-item-action pool newPool"><label for="newPool">Pool ID:</label><input class="form-control" id="newPool" type="text"><p id="poolErr" class="text-danger"></p></div>');
        let inputDiv = $('#newPool').parent();
        $('#newPool').keydown((e) => {
            if (e.keyCode == 13) { // Enter
                e.preventDefault();
                let poolId = $(e.target).val();
                if (poolId) {
                    $.post('/joinPool', {name: poolId}, (res) => {
                        let resData = JSON.parse(res);
                        if (!resData) {
                            $('#poolErr')[0].innerText = "Bad pool ID";
                        } else {
                            if( $('.disabler').prop('disabled')) {
                                $('.disabler').prop('disabled', false);
                                $('#noPool').html(''); 
                                $('#gains').show();
                            }
                            $('.port').removeClass('active');
                            let newPoolInput = $('.newPool');
                            let newPool = makeNewPool(resData);
                            newPoolInput.replaceWith(newPool);
                            newPool.click(poolClickHandler);
                            newPool.click();                            
                            $('#stocks').empty();
                        }
                    });
                } else {
                    // Remove
                   inputDiv.remove();
                }
            } else if (e.keyCode == 27) { // Escape
                e.preventDefault();
                // Remove
                inputDiv.remove();
            }
        });
        $('#newPool').focus();
    }
});

$('#createPool').click((e) => {
	let name = $('#name').val();
	let end = + new Date($('#end').val());
	let balance = $('#balance').val();

	if (!name) {
		$('#poolError').text('ERROR: Please give the pool a name.');
		return;
	}
	if (!end) {
		$('#poolError').text('ERROR: Please give the pool an end date.');
		return;
	}
	if (end <= + new Date()) {
		$('#poolError').text('ERROR: The pool end date must be after today.');
		return;
	}
	if (!balance) {
		$('#poolError').text('ERROR: Please give the pool a balance.');
		return;
	}
	let param = {
		name: name,
		end: end,
		balance: balance
	}

	$('#createPool').prop('disabled', true);
	$.post('/newPool', param, (res) => {
		console.log("HERE");
		// TODO Update pool sidebar
		$('#createPoolModal').modal('hide');
		$('#poolError').text('');
		$('#createPool').prop('disabled', false);
		$newPool = makeNewPool(name);
    $newPool.click(poolClickHandler);
		$('#pools').append($newPool);
    $newPool.click();
		// TODO add click handlers
	});
});