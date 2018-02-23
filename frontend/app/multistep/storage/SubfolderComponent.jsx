import React from 'react';
import PropTypes from 'prop-types';

class StorageSubfolderComponent extends React.Component {
    static propTypes = {
        valueWasSet: PropTypes.func.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            subfolder: "",
            clientpath: null
        }
    }

    render() {
        const selectedStorage = this.props.strgTypes[this.props.selectedType];

        if(!selectedStorage.hasSubFolders){
            return(<div>
                <h3>Storage Subfolder</h3>
                <p className="information">{selectedStorage.name} does not support subfolders</p>
            </div>)
        }

        return(<div>
                <h3>Storage Subfolder</h3>
                <table>
                    <tbody>
                    <tr>
                        <td>Subfolder path</td>
                        <td><input onChange={(event)=>this.setState({subfolder: event.target.value}, ()=>this.props.valueWasSet(this.state))}/></td>
                    </tr>
                    <tr>
                        <td>Client mount point (if any)</td>
                        <td><input onChange={(event)=>this.setState({clientpath: event.target.value}, ()=>this.props.valueWasSet(this.state))}/></td>
                    </tr>
                    </tbody>
                </table>
            </div>)
    }
}

export default StorageSubfolderComponent;
