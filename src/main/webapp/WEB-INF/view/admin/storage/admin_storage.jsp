<%@page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%><!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title><la:message key="labels.admin_brand_title" /> | <la:message
		key="labels.storage_configuration" /></title>
<jsp:include page="/WEB-INF/view/common/admin/head.jsp"></jsp:include>
</head>
<body class="hold-transition skin-blue sidebar-mini">
	<div class="wrapper">
		<jsp:include page="/WEB-INF/view/common/admin/header.jsp"></jsp:include>
		<jsp:include page="/WEB-INF/view/common/admin/sidebar.jsp">
			<jsp:param name="menuCategoryType" value="system" />
			<jsp:param name="menuType" value="storage" />
		</jsp:include>
		<div class="content-wrapper">
			<section class="content-header">
				<h1>
					<la:message key="labels.storage_configuration" />
				</h1>
				<jsp:include page="/WEB-INF/view/common/admin/crud/breadcrumb.jsp"></jsp:include>
			</section>
			<section class="content">
				<div class="row">
					<div class="col-md-12">
						<div class="box box-primary">
							<div class="box-header with-border">
								<h3 class="box-title">
									<la:message key="labels.storage_configuration" />
								</h3>
							</div>
							<!-- /.box-header -->
							<div class="box-body">
								<%-- Message --%>
								<div>
									<la:info id="msg" message="true">
										<div class="alert alert-info">${msg}</div>
									</la:info>
									<la:errors />
								</div>
								<%-- List --%>
								<div class="data-wrapper">
									<div class="row">
										<div class="col-sm-12">
											Path: ${f:h(endpoint)}/${f:h(bucket)}<c:forEach var="item" varStatus="s" items="${pathItems}">/<a href="${contextPath}/admin/storage/list/${f:u(item.id)}/">${f:h(item.name)}</a></c:forEach>
										</div>
										<div class="col-sm-12">
											<la:form action="/admin/storage/upload/" enctype="multipart/form-data" styleClass="form-inline">
												<div class="form-group">
													<label for="uploadFile"> <la:message key="labels.storage_upload_file" />
													</label> <input type="file" name="uploadFile" class="form-control" />
												</div>
												<button type="submit" class="btn btn-success" name="upload">
													<em class="fa fa-upload"></em>
													<la:message key="labels.storage_button_upload" />
												</button>
											</la:form>
										</div>
										<div class="col-sm-12">
											<table class="table table-bordered table-striped dataTable">
												<tbody>
													<tr>
														<th><la:message key="labels.storage_name" /></th>
														<th><la:message key="labels.storage_size" /></th>
														<th><la:message key="labels.storage_last_modified" /></th>
													</tr>
													<c:if test="${not empty path and not empty parentId}">
													<tr
														data-href="${contextPath}/admin/storage/list/${f:u(data.parentId)}/">
														<td>..</td>
														<td></td>
														<td></td>
													</tr></c:if>
													<c:if test="${not empty path and empty parentId}">
													<tr
														data-href="${contextPath}/admin/storage/">
														<td>..</td>
														<td></td>
														<td></td>
													</tr></c:if>
													<c:forEach var="data" varStatus="s" items="${fileItems}">
														<c:if test="${not data.directory}">
														<tr
															data-href="${contextPath}/admin/storage/download/${f:u(data.id)}/">
															<td>${f:h(data.name)}</td>
															<td>${f:h(data.size)}</td>
															<td>${f:h(data.lastModifed)}</td>
														</tr>
														</c:if><c:if test="${data.directory.booleanValue()}">
														<tr
															data-href="${contextPath}/admin/storage/list/${f:u(data.id)}/">
															<td>${f:h(data.name)}</td>
															<td></td>
															<td></td>
														</tr>
														</c:if>
													</c:forEach>
												</tbody>
											</table>
										</div>
									</div>
								</div>
								<!-- /.data-wrapper -->
							</div>
							<!-- /.box-body -->
						</div>
						<!-- /.box -->
					</div>
				</div>
			</section>
		</div>
		<jsp:include page="/WEB-INF/view/common/admin/footer.jsp"></jsp:include>
	</div>
	<jsp:include page="/WEB-INF/view/common/admin/foot.jsp"></jsp:include>
</body>
</html>
