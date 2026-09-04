/*
 * Created by zmzhou on 2021/2/24 16:50
 */

/**
 * 加密
 * @param content 加密内容
 * @returns {*}
 */
let encrypt = function (content) {
    let aseKey = "ws9ybUMn4F81t5oPKqJrqLKxERaYAS12"
    return CryptoJS.AES.encrypt(content, CryptoJS.enc.Utf8.parse(aseKey), {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.Pkcs7
    }).toString();
}

function showTips(msg) {
    $("#message_console").html(new Date().toLocaleTimeString() + "：" + msg);
}

function redirectToLogin() {
    var container = document.createElement('div');
    container.className = 'position-fixed top-50 start-50 translate-middle';
    container.style.zIndex = '9999';
    container.innerHTML =
        '<div class="toast" role="alert" aria-live="assertive" aria-atomic="true" data-bs-delay="2500" style="min-width:320px;">' +
        '<div class="toast-header bg-warning text-dark">' +
        '<i class="bi bi-exclamation-triangle-fill me-2"></i>' +
        '<strong class="me-auto">会话已过期</strong>' +
        '</div>' +
        '<div class="toast-body bg-warning-subtle text-dark py-3">登录已过期，正在跳转至登录页...</div>' +
        '</div>';
    document.body.appendChild(container);

    var toastEl = container.querySelector('.toast');
    var toast = new bootstrap.Toast(toastEl, { delay: 2500, autohide: true });
    toast.show();

    setTimeout(function () {
        window.opener = null;
        window.open('', '_self');
        window.close();
        if (window.top !== window.self) {
            window.top.location.href = '/';
        } else {
            window.location.href = '/';
        }
    }, 2500);
}

function checkErr(data) {
    if (!data) {
        redirectToLogin();
        return false;
    }
    if (data.code === 401) {
        redirectToLogin();
        return false;
    }
    if (data.code === 200) {
        showTips("操作成功！");
        return true;
    }
    showTips("操作失败！" + (data.msg || "未知错误"));
    return false;
}

$(document).ajaxError(function (event, xhr) {
    if (xhr.status === 401) {
        redirectToLogin();
    }
});

function uploadFile() {
    let val = $('#current_path').val();
    $("#path").val(val);
    let files = $("#file").get(0).files;
    let fileNames = [];
    for (let i = 0; i < files.length; i++) {
        fileNames.push(files[i].name)
    }
    let formData = new FormData(document.querySelector("#upload_form"));
    $.ajax({
        url: "/sftp/upload",
        type: "POST",
        data: formData,
        processData: false,
        contentType: false,
        success: function (res) {
            if (!checkErr(res)) return;
            let tips = fileNames.join(",") + res.data;
            showTips(tips);
            let $tree = $('#file_tree').jstree(true);
            $tree.select_node(val);
            $tree.open_node($tree.get_node(val));
            alert(tips);
        }
    });
}

function refreshTree() {
    let val = $('#current_path').val();
    let $tree = $('#file_tree').jstree(true);
    $tree.select_node(val);
    $tree.open_node($tree.get_node(val));
}