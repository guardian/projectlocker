import React from 'react';
import axios from 'axios';

class StorageListEntry extends React.Component {
    constructor(props){
        super(props);
        this.state = {
            'hovered': false,
            'selected': false
        }
    }

    mouseover(event) {
        this.setState({'hovered': true});
    }

    mouseout(event) {
        this.setState({'hovered': false});
    }

    clicked(event) {
        alert("boo!");
    }

    render() {
        const storage = this.props.storage;
        return (<tr onMouseOver={this.mouseover.bind(this)}
                    onMouseOut={this.mouseout.bind(this)}
                    onClick={this.clicked.bind(this)}
                    className={this.state.hovered ? "mouseover selectable-row":"selectable-row"}
                    >
            <td className="visible">{storage.id}</td>
            <td className="visible">{storage.storageType}</td>
            <td className="visible">{storage.rootpath}</td>
            <td className="visible">{storage.user}</td>
            <td className="visible">{storage.password}</td>
            <td className="visible">{storage.host}</td>
            <td className="visible">{storage.port}</td>
        </tr>);
    }
}

class StorageListComponent extends React.Component {
    constructor(props){
        super(props);
        this.state = {
            'storages': [],
            'hovered': false
        };
    }

    componentDidMount() {
        this.reload();
    }

    reload(){
        let component = this;

        axios.get('/storage').then(function(result){
            component.setState({
                'storages': result.data.result
            });
        }).catch(function (error) {
            console.error(error);
        });
    }

    render() {
        return (<table className="dashboardpanel">
            <thead>
            <tr className="dashboardheader">
                <td className="visible">ID</td>
                <td className="visible">Type</td>
                <td className="visible">Root path</td>
                <td className="visible">User</td>
                <td className="visible">Password</td>
                <td className="visible">Host</td>
                <td className="visible">Port</td>
            </tr>
            </thead>
            <tbody>
            {this.state.storages.map(function(storage){
                return <StorageListEntry key={storage.id} storage={storage}/>
            })}
            </tbody>
        </table>);
    }
}

export default StorageListComponent;