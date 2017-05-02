import React from 'react';

class StorageSubfolderComponent extends React.Component {
    constructor(props){
        super(props);

        this.state = {
            subfolder: ""
        }
    }

    componentDidUpdate(prevProps,prevState){
        if(prevState!=this.state) this.props.valueWasSet(this.state.subfolder);
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
                        <td><input onChange={(event)=>this.setState({subfolder: event.target.value})}/></td>
                    </tr>
                    </tbody>
                </table>
            </div>)
    }
}

export default StorageSubfolderComponent;
