$(function () {
    $("[name='pageRows']").change(function () {
        $("[name='frmPageRows']").attr({
            'method': 'POST',
            'action': 'pageRows',
        }).submit()
    })
})