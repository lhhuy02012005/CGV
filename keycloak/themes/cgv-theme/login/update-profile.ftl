<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('firstName','lastName','username','email'); section>
    <#if section = "header">
        ${msg("updateProfileTitle")}
    <#elseif section = "form">
        <form id="kc-update-profile-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">

            <!-- Giữ lại các trường ẩn hoặc tự động xử lý email/username nếu cần -->
            <input type="hidden" id="username" name="username" value="${(user.username!'')}" />
            <input type="hidden" id="email" name="email" value="${(user.email!'')}" />

            <!-- Trường nhập duy nhất: Full Name -->
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="fullName" class="${properties.kcLabelClass!}">Họ và tên (Full Name)</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="fullName" name="fullName" value="${(user.attributes.fullName[0])!'${(user.firstName!'')} ${(user.lastName!'')}'}" class="${properties.kcInputClass!}" autofocus autocomplete="name" />
                </div>
            </div>

            <!-- Các input ẩn chứa firstName và lastName thực tế mà Keycloak yêu cầu -->
            <input type="hidden" id="firstName" name="firstName" value="${(user.firstName!'')}" />
            <input type="hidden" id="lastName" name="lastName" value="${(user.lastName!'')}" />

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}" onclick="splitFullName()" />
                </div>
            </div>
        </form>

        <!-- Đoạn Script nhỏ tự động tách Full Name thành First Name và Last Name trước khi gửi lên server -->
        <script>
            function splitFullName() {
                var fullNameInput = document.getElementById('fullName').value.trim();
                var parts = fullNameInput.split(/\s+/); // Tách theo khoảng trắng

                var firstName = "";
                var lastName = "";

                if (parts.length === 1) {
                    firstName = parts[0];
                    lastName = "";
                } else if (parts.length > 1) {
                    lastName = parts.pop(); // Từ cuối cùng làm Last Name
                    firstName = parts.join(" "); // Các từ còn lại làm First Name
                }

                document.getElementById('firstName').value = firstName;
                document.getElementById('lastName').value = lastName;
            }
        </script>
    </#if>
</@layout.registrationLayout>