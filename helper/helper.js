$(document).ready(function() {

	$('a#start').ready(function(event) {
		document.getElementById('start').scrollIntoView(true);
	});

	$('button[type=button]#help').click(function(event) {
		window.location.href='help.html';
	});

	$('button[type=button]#reset').click(function(event) {
		window.location.href='.';
	});

	$('button[type=button]#permalink').click(function(event) {
		var permalink = document.getElementById('permalink');
		navigator.clipboard.writeText(permalink.getAttribute('link'));
		window.alert(permalink.getAttribute('message'));
	});

	$('button[type=button]#hide').ready(function(event) {
		var button = document.getElementById('hide');
		var isHidden = button.getAttribute('status') === 'hide';
		button.innerHTML = (isHidden) ? button.getAttribute('hidecontent') : button.getAttribute('showcontent');
		button.title = (isHidden) ? button.getAttribute('hidetitle') : button.getAttribute('showtitle');
		document.getElementById('dropbill_frame').style.display = (isHidden) ? 'none' : 'block';
	});

	$('button[type=button]#hide').click(function(event) {
		var button = document.getElementById('hide');
		var isHidden = button.getAttribute('status') === 'hide';
		button.setAttribute('status', ((isHidden) ? 'show' : 'hide'));
		button.innerHTML = (isHidden) ? button.getAttribute('showcontent') : button.getAttribute('hidecontent');
		button.title = (isHidden) ? button.getAttribute('showtitle') : button.getAttribute('hidetitle');
		document.getElementById('dropbill_frame').style.display = (isHidden) ? 'block' : 'none';
		document.getElementById('start').scrollIntoView(isHidden);
	});

	$('input[type=search].performSearch').keydown(function(event) {
		if (event.which == 13) {
			document.getElementById('search').click();
			event.preventDefault();
		}
	});

});
