var Table = ReactBootstrap.Table;
var Button = ReactBootstrap.Button;
var ButtonToolbar = ReactBootstrap.ButtonToolbar;
var Input = ReactBootstrap.Input;
var Grid = ReactBootstrap.Grid;
var Row = ReactBootstrap.Row;
var Col = ReactBootstrap.Col;
var Modal = ReactBootstrap.Modal;

var Server = React.createClass({displayName: "Server",
    handleDelete: function() {
        this.props.onDelete(this.props.server);
    },
    render: function() {
        return (
            React.createElement("tr", null, 
            React.createElement("td", null, this.props.index+1), 
            React.createElement("td", null, this.props.server.alias), 
            React.createElement("td", null, this.props.server.host, ":", this.props.server.port), 
            React.createElement("td", null, this.props.server.project), 
            React.createElement("td", null, 
            React.createElement(ButtonToolbar, null, 
            React.createElement(Button, {bsStyle: "danger", onClick: this.handleDelete}, "Delete")
            )
            )
            )
        );
    }
});

var ServerList = React.createClass({displayName: "ServerList",
    render: function() {
        var servers = this.props.servers.map(function (server, index) {
            return (
                React.createElement(Server, {index: index, server: server, onDelete: this.props.onDelete})
            );
        }.bind(this));
        return (
            React.createElement(Table, {striped: true, bordered: true, condensed: true, hover: true}, 
            React.createElement("thead", null, 
            React.createElement("colgroup", null, 
            React.createElement("col", {span: "1", width: "5%"}), 
            React.createElement("col", {span: "1", width: "35%"}), 
            React.createElement("col", {span: "1", width: "20%"}), 
            React.createElement("col", {span: "1", width: "20%"}), 
            React.createElement("col", {span: "1", width: "20%"})
            ), 
            React.createElement("tr", null, 
            React.createElement("th", null, "#"), 
            React.createElement("th", null, "Alias"), 
            React.createElement("th", null, "Host:Port"), 
            React.createElement("th", null, "Project"), 
            React.createElement("th", null, "Actions")
            )
            ), 
            React.createElement("tbody", null, 
            servers
            )
            )
        );
    }
});

var AddServerForm = React.createClass({displayName: "AddServerForm",
    render: function() {
        return (
            React.createElement("div", null, 
            React.createElement(Button, {onClick: this.props.onBeginAddServer}, "Add")
            )
        );
    }
});

var DeleteConfimationDialog = React.createClass({displayName: "DeleteConfimationDialog",
    handleOk: function() {
        this.props.onOk(this.props.server);
    },
    render: function() {
        var title = "Do you really want to delete '" + this.props.server.alias + "'?";
    return (
        React.createElement(Modal, {title: title, backdrop: false, animation: false, onRequestHide: this.props.onHide}, 
        React.createElement("div", {className: "modal-body"}, 
        this.props.server.alias, " at ", this.props.server.host, ":", this.props.server.port
        ), 
        React.createElement("div", {className: "modal-footer"}, 
        React.createElement(Button, {onClick: this.props.onCancel}, "Cancel"), 
        React.createElement(Button, {bsStyle: "danger", onClick: this.handleOk}, "Delete")
        )
        )
    );
}
});

var SettingsPage = React.createClass({displayName: "SettingsPage",
    getInitialState: function() {
        return {
            showList: true,
            servers: []
        };
    },
    beginAddServer: function() {
        this.setState({
            showList:false
        });
    },
    showList: function() {
        this.setState({
            showList:true
        });
    },
    saveServer: function(server) {
        $.post("/settings/servers/add",server,function() {
            this.loadServerList();
            this.showList();
        }.bind(this));
    },
    cancelEditServer: function() {
        this.showList();
    },
    loadServerList: function() {
        $.get('/settings/servers/list', function(response) {
            if (this.isMounted()) {
                this.setState({
                    servers: response.servers
                });
            }
        }.bind(this));
    },
    componentDidMount: function() {
        this.loadServerList();
    },
    handleDelete: function(server) {
        $.post('/settings/servers/delete', {id:server.id}, function (response) {
            this.hideDeleteConfirmationDialog();
            this.loadServerList();
        }.bind(this));
    },
    confirmDelete: function(server) {
        this.setState({
            showList: true,
            servers: this.state.servers,
            serverToDelete: server
        });
    },
    hideDeleteConfirmationDialog: function() {
        this.setState({
            serverToDelete: null
        });
    },
    cancelDelete: function() {
        this.hideDeleteConfirmationDialog();
    },
    render: function() {
        if (this.state.showList)
            return (
                React.createElement("div", null, 
                React.createElement(AddServerForm, {onBeginAddServer: this.beginAddServer}), 
                React.createElement("br", null), 
                React.createElement(ServerList, {servers: this.state.servers, onDelete: this.confirmDelete}), 
                this.state.serverToDelete != null
                    ? React.createElement(DeleteConfimationDialog, {
                    server: this.state.serverToDelete, 
                    onCancel: this.cancelDelete, 
                    onOk: this.handleDelete, 
                    onRequestHide: this.cancelDelete})
                    : null
                )
            );
        return (
            React.createElement("div", null, 
            React.createElement(EditServerForm, {onSave: this.saveServer, onCancel: this.cancelEditServer})
            )
        );
    }
});

var EditServerForm = React.createClass({displayName: "EditServerForm",
    handleSubmit: function(e) {
        e.preventDefault();
        this.props.onSave({
            alias: this.refs.alias.getInputDOMNode().value.trim(),
            host: this.refs.host.getInputDOMNode().value.trim(),
            port: this.refs.port.getInputDOMNode().value.trim(),
            project: this.refs.project.getInputDOMNode().value.trim(),
            username: this.refs.username.getInputDOMNode().value.trim(),
            password: this.refs.password.getInputDOMNode().value.trim()
        });
    },
    cancelEdit: function() {
        this.props.onCancel();
    },
    render: function() {
        return (
            React.createElement("form", {action: "", method: "POST", onSubmit: this.handleSubmit}, 
            React.createElement(Grid, null, 
            React.createElement(Row, null, 
            React.createElement(Col, {xs: 12, md: 6}, 

            React.createElement(Input, {type: "text", label: "Alias", ref: "alias", placeholder: "Enter text"}), 
            React.createElement(Input, {type: "text", label: "Host", ref: "host", placeholder: "Enter text"}), 
            React.createElement(Input, {type: "text", label: "Port", ref: "port", placeholder: "Enter text"}), 
            React.createElement(Input, {type: "text", label: "Project", ref: "project", placeholder: "Enter text"}), 
            React.createElement(Input, {type: "text", label: "Username", ref: "username", placeholder: "Enter text"}), 
            React.createElement(Input, {type: "text", label: "Password", ref: "password", placeholder: "Enter text"}), 
            React.createElement(ButtonToolbar, null, 
            React.createElement(Button, {bsStyle: "success", type: "submit"}, "Save"), 
            React.createElement(Button, {onClick: this.cancelEdit}, "Cancel")
            )
            )
            )
            )
            )
        );
    }
});

React.render(
    React.createElement(SettingsPage, null),
    document.getElementById('main-content')
);

