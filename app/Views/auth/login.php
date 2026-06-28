<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-md-5 col-lg-4">
            <div class="card shadow-sm border-0">
                <div class="card-body p-4">
                    <h1 class="h3 mb-3 text-center">PlayOnMyTV</h1>
                    <p class="text-muted text-center mb-4">Admin Portal Login</p>
                    <form method="post" action="#">
                        <?= csrf_field() ?>
                        <div class="mb-3">
                            <label for="email" class="form-label">Email</label>
                            <input id="email" name="email" type="email" class="form-control" placeholder="name@example.com">
                        </div>
                        <div class="mb-3">
                            <label for="password" class="form-label">Password</label>
                            <input id="password" name="password" type="password" class="form-control" placeholder="••••••••">
                        </div>
                        <button type="button" class="btn btn-primary w-100" disabled>Login Coming Soon</button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

