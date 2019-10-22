import React from 'react';
import PropTypes from 'prop-types';

class StorageTypeComponent extends React.Component {
    static propTypes = {
        strgTypes: PropTypes.array.isRequired,
        valueWasSet: PropTypes.func.isRequired,
        selectedType: PropTypes.number.isRequired,
        versionsAllowed: PropTypes.bool.isRequired,
        versionsAllowedChanged: PropTypes.func.isRequired
    };

    constructor(props){
        super(props);

        const initialStorage = this.props.strgTypes.length>0 ? this.props.strgTypes[0] : null;

        this.state = {
            selectedType: 0,
            disableVersions: initialStorage ? initialStorage.canVersion : true
        };

        this.selectorValueChanged = this.selectorValueChanged.bind(this);
    }

    selectorValueChanged(event){
        const actualStorage = this.props.strgTypes[event.target.value];
        console.log(actualStorage);

        this.setState({
            selectedType: event.target.value,
            disableVersions: actualStorage.hasOwnProperty("canVersion") ? !actualStorage.canVersion : true
        }, ()=>this.props.valueWasSet(parseInt(this.state.selectedType)));
    }

    render() {
        return(<div>
            <h3>Storage Type</h3>
            <p className="information">The first piece of information we need is what kind of storage to connect to.
                Different storages require different configuration options; currently we support a local disk, ObjectMatrix vault,
                or an S3 bucket.
            </p>
                <table><tbody>
                <tr><td>
                    <select id="storage_type_selector" value={this.props.selectedType} onChange={this.selectorValueChanged}>
                        {
                            this.props.strgTypes.map((typeInfo, index)=><option key={index} value={index}>{typeInfo.name}</option>)
                        }
                    </select>
                </td></tr>
                <tr><td>
                    <input type="checkbox" checked={this.props.versionsAllowed} onChange={evt=>this.props.versionsAllowedChanged(!this.props.versionsAllowed)}
                           id="chk-versions-allowed"
                           disabled={this.state.disableVersions}/>
                    <label htmlFor="chk-versions-allowed" style={{display: "inline", marginLeft:"0.4em", color: this.state.disableVersions ? "grey" : "black"}} >Enable versions</label>
                </td></tr>
                </tbody></table>
        </div>
        )

    }
}

export default StorageTypeComponent;
